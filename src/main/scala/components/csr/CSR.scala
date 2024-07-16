package csr

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

class CSRIO extends Bundle {
  // memory2
  val csrRead = new CsrReadIO

  // commit
  val excHappen = Input(new ExcHappenInfo)
  val csrWrite  = Input(new CSRWrite)
  val excJump   = Output(new BranchInfo)

  // to tlb
  val tlb = Vec(2, new CSRTLBIO)
}

class CSR extends Module {
  val io = IO(new CSRIO)

  val saved_info = RegInit(0.U.asTypeOf(new SingleInfo))
  when(!io.excHappen.info.bubble) {
    saved_info := io.excHappen.info
  }
  val info = Mux(io.excHappen.info.bubble, saved_info, io.excHappen.info)

  val CRMD    = new CRMD
  val PRMD    = new PRMD
  val EUEN    = new EUEN
  val ECFG    = new ECFG
  val ESTAT   = new ESTAT
  val ERA     = new ERA
  val BADV    = new BADV
  val EENTRY  = new EENTRY
  val TLBIDX  = new TLBIDX
  val TLBEHI  = new TLBEHI
  val TLBELO0 = new TLBELO0
  val TLBELO1 = new TLBELO1
  val ASID    = new ASID
  val PGDL    = new PGDL
  val PGDH    = new PGDH
  val PGD     = new PGD
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
  val TLBRENTRY = new TLBRENTRY
  // val CTAG      =
  val DMW0 = new DMW0
  val DMW1 = new DMW1

  val csrlist = Seq(
    CRMD,
    PRMD,
    EUEN,
    ECFG,
    ESTAT,
    ERA,
    BADV,
    EENTRY,
    TLBIDX,
    TLBEHI,
    TLBELO0,
    TLBELO1,
    ASID,
    PGDL,
    PGDH,
    PGD,
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
    TLBRENTRY,
    // CTAG,
    DMW0,
    DMW1,
  )

  // 读 or 写
  io.csrRead.data := 0.U
  for (x <- csrlist) {
    when(io.csrRead.addr === x.id) {
      io.csrRead.data := x.info.asUInt
      when(x.id === CSRCodes.TICLR) {
        io.csrRead.data := 0.U
      }
    }
  }

  val conuter_run    = WireDefault(true.B)
  val is_soft_int_ex = WireDefault(false.B)

  when(io.csrWrite.we) {
    for (x <- csrlist) {
      when(io.csrWrite.waddr === x.id) {
        val wdata = writeMask(io.csrWrite.wmask, x.info.asUInt, io.csrWrite.wdata)
        x.write(wdata)
        // 清除中断位 当有写1的行为
        when(x.id === CSRCodes.TICLR && wdata(0) === 1.U) {
          ESTAT.info.is_11 := false.B
        }
        when(x.id === CSRCodes.TCFG) {
          conuter_run       := false.B
          TVAL.info.timeval := wdata(COUNT_N - 1, 2) ## 3.U(2.W)
        }
        when(x.id === CSRCodes.ESTAT && wdata(1, 0) =/= 0.U) {
          is_soft_int_ex := true.B
        }
      }
    }
  }

  when(TCFG.info.en) {
    when(TVAL.info.timeval === 0.U) {
      TVAL.info.timeval := Mux(TCFG.info.preiodic, TCFG.info.initval ## 3.U(2.W), 0.U)
    }.elsewhen(conuter_run) {
      TVAL.info.timeval := TVAL.info.timeval - 1.U
    }
  }

  val TVAL_edge = ShiftRegister(TVAL.info.timeval, 1)
  when(TCFG.info.en && TVAL.info.timeval === 0.U && TVAL_edge === 1.U) {
    ESTAT.info.is_11 := true.B
  }

  val any_exc = Cat(ESTAT.info.is_12, ESTAT.info.is_11, ESTAT.info.is_9_2, ESTAT.info.is_1_0) &
    Cat(ECFG.info.lie_12_11, ECFG.info.lie_9_0)
  val is_tlb_exc    = ECodes.istlbException(info.exc_type)
  val is_tlb_refill = info.exc_type === ECodes.TLBR

  val start = io.excHappen.start || (any_exc.orR && CRMD.info.ie)

  // 例外跳转
  io.excJump := WireDefault(0.U.asTypeOf(new BranchInfo))
  when(start) {
    PRMD.info.pplv := CRMD.info.plv

    PRMD.info.pie := CRMD.info.ie
    CRMD.info.plv := 0.U
    CRMD.info.ie  := 0.U
    // 中断>例外>tlb例外的优先级，不过本身的设计保证例外和tlb例外不会同时发生，且普通例外优先
    ESTAT.info.ecode := MuxCase(
      info.exc_type,
      List(
        ESTAT.info.is_1_0.orR -> ECodes.INT,
        ESTAT.info.is_9_2.orR -> ECodes.INT,
        ESTAT.info.is_11      -> ECodes.INT,
        ESTAT.info.is_12      -> ECodes.INT,
      ),
    )
    ESTAT.info.esubcode := Mux(info.exc_type === ECodes.ADEM, 1.U, 0.U)
    // 软中断的ERApc是下一个pc, TODO:中断标记在某个指令上
    ERA.info.pc := Mux(is_soft_int_ex, info.pc_add_4, info.pc)
    BADV.info.vaddr := MateDefault(
      info.exc_type,
      BADV.info.vaddr,
      List(
        ECodes.ADEF -> info.exc_vaddr,
        ECodes.ADEM -> info.exc_vaddr,
        ECodes.ALE  -> info.exc_vaddr,
      ),
    )

    io.excJump.en  := true.B
    io.excJump.tar := EENTRY.info.asUInt

    when(is_tlb_exc) {
      when(is_tlb_refill) {
        CRMD.info.da   := true.B
        CRMD.info.pg   := false.B
        io.excJump.tar := TLBRENTRY.info.asUInt
      }
      TLBEHI.info.vppn := info.exc_vaddr(31, 13)
    }
  }

  when(io.excHappen.end) {
    CRMD.info.plv := PRMD.info.pplv
    CRMD.info.ie  := PRMD.info.pie

    io.excJump.en  := true.B
    io.excJump.tar := ERA.info.pc
  }

  for (i <- 0 until 2) {
    io.tlb(i).is_direct := CRMD.info.da && !CRMD.info.pg
    io.tlb(i).asid      := ASID.info
    io.tlb(i).crmd      := CRMD.info
    io.tlb(i).dmw(0)    := DMW0.info
    io.tlb(i).dmw(1)    := DMW1.info
  }
}
