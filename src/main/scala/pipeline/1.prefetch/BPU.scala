package pipeline

import chisel3._
import chisel3.util._

import bundles._
import const.Parameters._
import const.Predict._
import const._
import func.Functions._
import memory.cache._
import chisel3.util.experimental.BoringUtils

/*
ARAT中维护了寄存器映射表的有效位，可在分支预测失败时恢复到重命名单元中。
同时，ARAT还维护了分支预测器返回地址栈的栈顶指针，在分支预测失败时，也会恢复到分支预测器中，对分支预测正确率有一定提升。
 */

class BPUIO extends Bundle {
  val preFetch     = Flipped(new PreFetchBPUIO)
  val fetch        = Flipped(new FetchBPUIO)
  val succeed_time = if (Config.statistic_on) Some(Output(UInt(DATA_WIDTH.W))) else None
  val total_time   = if (Config.statistic_on) Some(Output(UInt(DATA_WIDTH.W))) else None
}

class BPU extends Module {
  val io = IO(new BPUIO)

  val pFF :: pF :: pTT :: pT :: Nil = Enum(COUNTER_WIDTH)

  // the two table are same COMPLETELY, just apply two read portals
  // val BHT = RegInit(VecInit.fill(FETCH_DEPTH)(VecInit(Seq.fill(INDEX_WIDTH)(0.U(HISTORY_LENGTH.W)))))
  val BHT = VecInit.fill(FETCH_DEPTH + 1)(
    Module(new xilinx_simple_dual_port_1_clock_ram_read_first(HISTORY_LENGTH, INDEX_WIDTH)).io,
  )
  val PHT = RegInit(VecInit.fill(FETCH_DEPTH)(VecInit(Seq.fill(HISTORY_WIDTH)(pF))))
  val BTB = VecInit.fill(FETCH_DEPTH)(
    Module(new xilinx_simple_dual_port_1_clock_ram_read_first(BTB_INFO_LENGTH, BTB_INDEX_WIDTH)).io,
  )
  val RAS = RegInit(VecInit(Seq.fill(RAS_DEPTH)(0x1c000000.U(ADDR_WIDTH.W))))

  for (i <- 0 until FETCH_DEPTH + 1) {
    BHT(i).clka := clock
    if (i < 2) {
      BHT(i).addrb := io.preFetch.pcGroup(i)(INDEX_LENGTH + 1, 2)
    } else {
      BHT(i).addrb := io.preFetch.train.index
    }
    BHT(i).wea   := false.B
    BHT(i).addra := RegNext(io.preFetch.train.index)
    BHT(i).dina  := 0.U
  }

  val trainDirection = io.preFetch.train.realDirection
  val oldBHR         = BHT(FETCH_DEPTH).doutb

  // BHT and PHT train
  when(RegNext(io.preFetch.train.isbr)) {
    for (i <- 0 until FETCH_DEPTH + 1) {
      BHT(i).wea  := true.B
      BHT(i).dina := Cat(oldBHR(HISTORY_LENGTH - 2, 0), RegNext(trainDirection))
      if (i < 2) {
        switch(PHT(i)(oldBHR)) {
          is(pFF) {
            PHT(i)(oldBHR) := Mux(RegNext(trainDirection), pF, pFF)
          }
          is(pF) {
            PHT(i)(oldBHR) := Mux(RegNext(trainDirection), pT, pFF)
          }
          is(pT) {
            PHT(i)(oldBHR) := Mux(RegNext(trainDirection), pTT, pF)
          }
          is(pTT) {
            PHT(i)(oldBHR) := Mux(RegNext(trainDirection), pTT, pT)
          }
        }
      }
    }
  }

  val index = VecInit.tabulate(FETCH_DEPTH)(i => BHT(i).doutb)

  // BTB: pc-relative or call
  for (i <- 0 until FETCH_DEPTH) {
    BTB(i).clka  := clock
    BTB(i).addrb := io.preFetch.pcGroup(i)(BTB_INDEX_LENGTH + 1, 2)
    BTB(i).wea   := false.B
    BTB(i).addra := 0.U
    BTB(i).dina  := 0.U
  }
  when(io.preFetch.train.isbr && io.preFetch.train.br.en) { // only when predict failed
    for (i <- 0 until FETCH_DEPTH) {
      BTB(i).wea   := true.B
      BTB(i).addra := io.preFetch.train.pc(BTB_INDEX_LENGTH + 1, 2)
      BTB(i).dina := Cat(
        true.B,
        io.preFetch.train.pc(ADDR_WIDTH - 1, ADDR_WIDTH - BTB_TAG_LENGTH),
        io.preFetch.train.br.tar,
        io.preFetch.train.isCALL,
        io.preFetch.train.isReturn,
      )
    }
  }
  val validVec = VecInit.tabulate(FETCH_DEPTH)(i => BTB(i).doutb(BTB_INFO_LENGTH - 1))
  val tagVec =
    VecInit.tabulate(FETCH_DEPTH)(i => BTB(i).doutb(BTB_INFO_LENGTH - 2, BTB_INFO_LENGTH - 1 - BTB_TAG_LENGTH))
  val BTBTarVec =
    VecInit.tabulate(FETCH_DEPTH)(i => BTB(i).doutb(BTB_INFO_LENGTH - BTB_TAG_LENGTH - 2, BTB_FLAG_LENGTH))
  val isCALLVec   = VecInit.tabulate(FETCH_DEPTH)(i => BTB(i).doutb(1))
  val isReturnVec = VecInit.tabulate(FETCH_DEPTH)(i => BTB(i).doutb(0))
  val BTBHitVec =
    VecInit.tabulate(FETCH_DEPTH)(i => validVec(i) && tagVec(i) === ShiftRegister(io.preFetch.pcGroup(i)(ADDR_WIDTH - 1, ADDR_WIDTH - BTB_TAG_LENGTH), 1))

  val predictDirection = VecInit.tabulate(FETCH_DEPTH)(i => PHT(i)(index(i))(1) && RegNext(io.preFetch.pcValid(i)) && BTBHitVec(i))

  // RAS: return
  val top         = RegInit(0.U(RAS_WIDTH.W))
  val top_add_1   = top + 1.U
  val top_minus_1 = top - 1.U
  val meetCALLVec = VecInit.tabulate(FETCH_DEPTH)(i => isCALLVec(i) && ShiftRegister(io.preFetch.pcValid(i), 1))
  when(meetCALLVec(0) && ShiftRegister(io.preFetch.valid, 1) && predictDirection(0)) {
    top      := top_add_1
    RAS(top) := ShiftRegister(io.preFetch.npcGroup(0), 1)
  }.elsewhen(meetCALLVec(1) && ShiftRegister(io.preFetch.valid, 1) && predictDirection(1)) {
    top      := top_add_1
    RAS(top) := ShiftRegister(io.preFetch.npcGroup(1), 1)
  }
  val RASHitVec = VecInit.tabulate(FETCH_DEPTH)(i => isReturnVec(i))
  // RASHitVec := VecInit(false.B, false.B)

  // predict
  io.fetch.predict.en    := true.B
  io.fetch.predict.tar   := 0.U
  io.fetch.firstInstJump := false.B
  when(predictDirection(0)) {
    when(RASHitVec(0) && ShiftRegister(io.preFetch.valid, 1)) {
      io.fetch.firstInstJump := true.B
      io.fetch.predict.tar   := RAS(top_minus_1)
      top                    := top_minus_1
    }.elsewhen(BTBHitVec(0)) {
      io.fetch.firstInstJump := true.B
      io.fetch.predict.tar   := BTBTarVec(0)
    }.otherwise {
      io.fetch.predict.en := false.B
    }
  }.elsewhen(predictDirection(1)) {
    when(RASHitVec(1) && ShiftRegister(io.preFetch.valid, 1)) {
      io.fetch.predict.tar := RAS(top_minus_1)
      top                  := top_minus_1
    }.elsewhen(BTBHitVec(1)) {
      io.fetch.predict.tar := BTBTarVec(1)
    }.otherwise {
      io.fetch.predict.en := false.B
    }
  }.otherwise {
    io.fetch.predict.en := false.B
  }

  if (Config.staticPredict) {
    io.fetch.predict.en := false.B
  }

  // count
  if (Config.statistic_on) {
    // not sure
    val tot_time     = RegInit(0.U(32.W))
    val succeed_time = RegInit(0.U(32.W))
    when(io.preFetch.train.isbr) {
      tot_time := tot_time + 1.U
      when(!io.preFetch.train.br.en) {
        succeed_time := succeed_time + 1.U
      }
    }
    io.succeed_time.get := succeed_time
    io.total_time.get   := tot_time
    dontTouch(trainDirection)
    dontTouch(tot_time)
    dontTouch(succeed_time)
    dontTouch(validVec)
    dontTouch(tagVec)
    dontTouch(BTBTarVec)
    dontTouch(isCALLVec)
    dontTouch(meetCALLVec)
    dontTouch(isReturnVec)
    dontTouch(BTBHitVec)
    dontTouch(predictDirection)
  }
}
