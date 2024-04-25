package stages

import chisel3._
import chisel3.util._

import isa._
import config._
import controller._
import config.Functions._

class IE_IO extends Bundle with Parameters {
  val from = Flipped(DecoupledIO(new info))
  val to   = DecoupledIO(new info)

  val es          = Output(new hazardData)
  val ds_reg_info = Input(new dsRegInfo)

  // ** to sram
  val data_sram_en    = Output(Bool())
  val data_sram_we    = Output(UInt(DATA_WIDTH_B.W))
  val data_sram_addr  = Output(UInt(ADDR_WIDTH.W))
  val data_sram_wdata = Output(UInt(INST_WIDTH.W))
}

class IE extends Module with Parameters {
  val io = IO(new IE_IO)

  // 与上一流水级握手，获取上一流水级信息
  val info = ConnectGetBus(io.from, io.to)

  val es_mem_we = info.func_type === FuncType.mem && info.op_type === MemOpType.write
  val es_mem_re = info.func_type === FuncType.mem && info.op_type === MemOpType.read

  when(es_mem_re && io.ds_reg_info.addr.exists(_ === info.dest)) {
    io.from.ready := false.B
    info          := WireDefault(0.U.asTypeOf(new info))
  }

  // 调用DIV获得运算结果
  val div = Module(new DIV)
  div.io.div_op   := info.op_type
  div.io.div_src1 := info.src1
  div.io.div_src2 := info.src2
  div.io.start    := info.func_type === FuncType.div
  val div_result = div.io.div_result

  val is_cal = WireDefault(true.B)

  when(info.func_type === FuncType.div) {
    io.from.ready := false.B
    is_cal        := false.B
    when(div.io.complete) {
      info.func_type := FuncType.nondiv
    }
  }

  // 调用MUL获得运算结果
  val mul = Module(new MUL)
  mul.io.mul_op   := info.op_type
  mul.io.mul_src1 := info.src1
  mul.io.mul_src2 := info.src2

  when(info.func_type === FuncType.mul) {
    io.from.ready := false.B
    is_cal        := false.B
    info.func_type := FuncType.nonmul
  }

  // 调用ALU获得运算结果
  val alu = Module(new ALU)
  alu.io.alu_op   := info.op_type
  alu.io.alu_src1 := info.src1
  alu.io.alu_src2 := info.src2

  val result = MateDefault(
    info.func_type,
    alu.io.alu_result,
    List(
      FuncType.div -> div.io.div_result,
      FuncType.nondiv -> div.io.div_result,
      FuncType.mul -> mul.io.mul_result,
      FuncType.nonmul -> mul.io.mul_result,
    ),
  )

  val to_info = WireDefault(0.U.asTypeOf(new info))
  to_info        := info
  to_info.result := result
  to_info.is_wf  := info.is_wf && is_cal
  io.to.bits     := to_info

  io.es.we   := to_info.is_wf
  io.es.addr := to_info.dest
  io.es.data := to_info.result

  io.data_sram_en    := true.B
  io.data_sram_we    := Fill(DATA_WIDTH_B, es_mem_we & io.to.valid)
  io.data_sram_addr  := result
  io.data_sram_wdata := info.rkd_value // 只有st的rd被写入
}
