package pipeline

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._
import isa.PipelineType

class DecodeTopIO extends StageBundle {}

class DecodeTop extends Module {
  val io   = IO(new DecodeTopIO)
  val busy = WireDefault(0.U.asTypeOf(new BusyInfo))
  val from = stageConnect(io.from, io.to, busy)

  val info         = from._1.bits
  val valid_signal = from._2
  flushWhen(from._1, io.flush)

  val dec     = VecInit.fill(ISSUE_WIDTH)(Module(new Decoder).io)
  val to_info = WireDefault(0.U.asTypeOf(new DualInfo))

  for (i <- 0 until ISSUE_WIDTH) {
    dec(i).pc   := info(i).pc
    dec(i).inst := info(i).inst

    to_info.bits(i).pc           := info(i).pc
    to_info.bits(i).inst         := info(i).inst
    to_info.bits(i).func_type    := dec(i).func_type
    to_info.bits(i).op_type      := dec(i).op_type
    to_info.bits(i).imm          := dec(i).imm
    to_info.bits(i).src1IsZero   := dec(i).src1IsZero
    to_info.bits(i).src1Ispc     := dec(i).src1Ispc
    to_info.bits(i).src2IsFour   := dec(i).src2IsFour
    to_info.bits(i).src2IsImm    := dec(i).src2IsImm
    to_info.bits(i).rjInfo.areg  := dec(i).rj
    to_info.bits(i).rkInfo.areg  := dec(i).rk
    to_info.bits(i).rdInfo.areg  := dec(i).rd
    to_info.bits(i).iswf         := dec(i).iswf && !info(i).bubble
    to_info.bits(i).isReadCsr    := dec(i).isReadCsr
    to_info.bits(i).isWriteCsr   := dec(i).isWriteCsr
    to_info.bits(i).csr_addr     := dec(i).csrReg
    to_info.bits(i).isCALL       := dec(i).isCALL
    to_info.bits(i).isReturn     := dec(i).isReturn
    to_info.bits(i).predict      := info(i).predict
    to_info.bits(i).exc_type     := Mux(info(i).exc_type =/= ECodes.NONE, info(i).exc_type, dec(i).exc_type)
    to_info.bits(i).exc_vaddr    := info(i).pc
    to_info.bits(i).pipelineType := Mux(to_info.bits(i).exc_type =/= ECodes.NONE, PipelineType.nop, dec(i).pipelineType)
  }

  io.to.bits := Mux(io.flush, 0.U.asTypeOf(new DualInfo), to_info)
}
