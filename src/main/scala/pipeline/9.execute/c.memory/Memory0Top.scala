package pipeline

import chisel3._
import chisel3.util._

import isa._
import const._
import bundles._
import func.Functions._
import const.Parameters._

class Memory0TopIO extends SingleStageBundle {
  val tlb    = new Stage0TLBIO
  val dCache = new Mem0DCacheIO
  val mem2   = new ToMem2ForwardIO
}

class Memory0Top extends Module {
  val io   = IO(new Memory0TopIO)
  val busy = WireDefault(false.B)
  val raw  = stageConnect(io.from, io.to, busy, io.flush, true)

  val info  = raw._1
  val valid = raw._2
  val res   = WireDefault(info)

  // forward
  io.mem2.actualStore := info.rollback && info.writeInfo.requestInfo.rbType
  io.mem2.addr        := info.writeInfo.requestInfo.addr
  io.mem2.data        := info.writeInfo.requestInfo.wdata
  io.mem2.strb        := info.writeInfo.requestInfo.wstrb

  // calculate csr_wmask
  val is_xchg = info.func_type === FuncType.csr && info.op_type === CsrOpType.xchg
  res.csr_wmask := Mux(is_xchg, info.rjInfo.data, ALL_MASK.U)

  val va = Mux(info.rollback, info.writeInfo.requestInfo.addr, info.rjInfo.data + info.imm)

  // tlb
  io.tlb.va      := va
  io.tlb.memType := Mux(MemOpType.isread(info.op_type), memType.load, memType.store)
  val hitVec   = io.tlb.hitVec
  val isDirect = io.tlb.isDirect
  val directpa = io.tlb.directpa

  // dcache
  io.dCache.addr := va

  res.va       := va
  res.hitVec   := hitVec
  res.isDirect := isDirect
  res.pa       := directpa
  io.to.bits   := res

  if (Config.debug_on) {
    dontTouch(info.rjInfo.data)
    dontTouch(info.rkInfo.data)
    dontTouch(info.rdInfo.data)
  }
}
