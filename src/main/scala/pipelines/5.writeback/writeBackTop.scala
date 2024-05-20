package stages

import chisel3._
import chisel3.util._

import isa._
import bundles._
import const.ECodes
import const.CSRCodes
import const.Parameters._
import Funcs.Functions._

class WriteBackTopIO extends StageBundle {
  val forward_data = Output(new ForwardData)
  val gr_write     = Output(new GRWrite)
  // val csr_reg_read = new csrRegRead
  val csr_write = Output(new CSRWrite)
  val debug_wb  = Output(new debug_wb)

  val exc_happen   = Output(new excHappen)
  val flush_by_csr = Input(Bool())
}

class WriteBackTop extends Module {
  val io   = IO(new WriteBackTopIO)
  val busy = WireDefault(false.B)
  val info = StageConnect(io.from, io.to, busy)
  when(io.flush) {
    info        := 0.U.asTypeOf(new info)
    info.bubble := true.B
  }

  val exc_en = info.exc_type =/= ECodes.NONE && io.to.valid && !info.bubble

  io.gr_write.we     := info.iswf
  io.gr_write.waddr  := info.wfreg
  io.gr_write.wdata  := info.result
  io.csr_write.we    := info.csr_iswf
  io.csr_write.wmask := info.csr_wmask
  io.csr_write.waddr := info.csr_addr
  io.csr_write.wdata := info.rd

  io.exc_happen.start := WireDefault(false.B)
  io.exc_happen.end   := WireDefault(false.B)
  when(exc_en && info.exc_type =/= ECodes.ertn) {
    io.exc_happen.start := true.B
  }.elsewhen(exc_en) {
    io.exc_happen.end := true.B
  }
  io.exc_happen.info := info

  val to_info = WireDefault(0.U.asTypeOf(new info))
  to_info := info
  when(io.flush) {
    to_info        := 0.U.asTypeOf(new info)
    to_info.bubble := true.B
  }
  io.to.bits := to_info

  io.flush_apply := io.flush_by_csr && io.to.valid

  Forward(to_info, io.forward_data)

  io.debug_wb.pc       := info.pc
  io.debug_wb.rf_we    := Fill(4, info.iswf && io.to.valid)
  io.debug_wb.rf_wnum  := info.wfreg
  io.debug_wb.rf_wdata := io.gr_write.wdata
}
