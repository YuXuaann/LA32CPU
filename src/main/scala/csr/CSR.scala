package csr

import chisel3._
import chisel3.util._

import isa._
import stages._
import config._
import config.Functions._

class CSR_IO extends Bundle with Parameters {
  // from ds
  val re     = Input(Bool())
  val raddr  = Input(UInt(14.W))
  val rf_bus = Input(new rf_bus)

  // from wb
  val info  = Input(new info)
  val start = Input(Bool())
  val end   = Input(Bool())

  // to ds
  val rdata = Output(UInt(DATA_WIDTH.W))

  // to fs
  val exc_bus = Output(new exc_bus)
}

class CSR extends Module with Parameters {
  val io = IO(new CSR_IO)

  val CRMD   = new CRMD
  val PRMD   = new PRMD
  val EUEN   = new EUEN
  val ECFG   = new ECFG
  val ESTAT  = new ESTAT
  val ERA    = new ERA
  val BADV   = new BADV
  val EENTRY = new EENTRY
  // val TLBIDX    =
  // val TLBEHI    =
  // val TLBELO0   =
  // val TLBELO1   =
  // val ASID      =
  // val PGDL      =
  // val PGDH      =
  // val PGD       =
  // val CPUID     =
  val SAVE0 = new SAVE0
  val SAVE1 = new SAVE1
  val SAVE2 = new SAVE2
  val SAVE3 = new SAVE3
  val TID   = new TID
  val TCFG  = new TCFG
  val TVAL  = new TVAL
  val TICLR = new TICLR
  // val LLBCTL    =
  // val TLBRENTRY =
  // val CTAG      =
  // val DMW0      =
  // val DMW1      =

  val csrlist = Seq(
    CRMD,
    PRMD,
    EUEN,
    ECFG,
    ESTAT,
    ERA,
    BADV,
    EENTRY,
    // TLBIDX,
    // TLBEHI,
    // TLBELO0,
    // TLBELO1,
    // ASID,
    // PGDL,
    // PGDH,
    // PGD,
    // CPUID,
    SAVE0,
    SAVE1,
    SAVE2,
    SAVE3,
    TID,
    TCFG,
    TVAL,
    TICLR,
    // LLBCTL,
    // TLBRENTRY,
    // CTAG,
    // DMW0,
    // DMW1,
  )

  // 读 or 写
  io.rdata := 0.U
  when(io.re) {
    for (x <- csrlist) {
      when(io.raddr === x.id) {
        io.rdata := x.info.asUInt
        when(x.id === CSR.TICLR) {
          io.rdata := 0.U
        }
      }
    }
  }


  when(io.rf_bus.we) {
    for (x <- csrlist) {
      when(io.rf_bus.waddr === x.id) {
        val wdata = writeMask(io.rf_bus.wmask, x.info.asUInt, io.rf_bus.wdata)
        x.write(wdata)
        // 清除中断位 当有写1的行为
        when(x.id === CSR.TICLR && wdata === 1.U) {
          ESTAT.info.is_11 := false.B
        }
      }
    }
  }



  val timecnt_exc  = WireDefault(false.B)
  val counter      = RegInit(0.U(COUNT_N.W))
  val init_val     = Mux(TCFG.info.preiodic, TCFG.info.initval, 0.U)
  val cnt_flag     = RegInit(true.B)
  val timeexc_flag = RegInit(true.B)
  when(TCFG.info.en) {
    when(counter === 0.U) {
      when(cnt_flag) {
        cnt_flag := false.B
      }.otherwise {
        when(timeexc_flag) {
          timeexc_flag  := false.B
          timecnt_exc   := true.B
          ESTAT.info.is_11 := true.B
        }
      }
      counter := init_val
    }.otherwise {
      counter := counter - 1.U
    }
  }.otherwise {
    cnt_flag := true.B
  }
  when(cnt_flag) {
    counter := TCFG.info.initval // 它说要补两0？
  }
  // to do:TICLR.clr的逻辑控制
  TVAL.info.timeval := counter
  val start = io.start || (timecnt_exc && ECFG.info.lie_12_11(0).asBool && CRMD.info.ie)

  // 不可写区域
  CRMD.info.zero   := 0.U
  PRMD.info.zero   := 0.U
  EUEN.info.zero   := 0.U
  ECFG.info.zero1  := 0.U
  ECFG.info.zero2  := 0.U
  ESTAT.info.zero1 := 0.U
  ESTAT.info.zero2 := 0.U
  EENTRY.info.zero := 0.U
  TCFG.info.zero   := 0.U
  TVAL.info.zero   := 0.U
  TICLR.info.zero  := 0.U

  // 例外跳转
  io.exc_bus := WireDefault(0.U.asTypeOf(new exc_bus))
  when(start) {
    PRMD.info.pplv := CRMD.info.plv
    PRMD.info.pie  := CRMD.info.ie
    CRMD.info.plv  := 0.U
    CRMD.info.ie   := 0.U
    ESTAT.info.ecode := MuxCase(
      io.info.exc_type,
      List(
        timecnt_exc -> ECodes.INT,
      ),
    )
    ERA.info.pc := io.info.pc

    io.exc_bus.en := true.B
    io.exc_bus.pc := EENTRY.info.asUInt
  }

  when(io.end) {
    // timeexc_flag  := true.B
    CRMD.info.plv := PRMD.info.pplv
    CRMD.info.ie  := PRMD.info.pie

    io.exc_bus.en := true.B
    io.exc_bus.pc := ERA.info.pc
  }
}
