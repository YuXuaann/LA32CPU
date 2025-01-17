package bundles

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

import pipeline._
import controller._

class RobInfo extends Bundle {
  val done = Bool()
  val pc   = UInt(ADDR_WIDTH.W)

  val wen   = Bool()
  val areg  = UInt(AREG_WIDTH.W)
  val preg  = UInt(PREG_WIDTH.W)
  val opreg = UInt(PREG_WIDTH.W)
  val wdata = UInt(DATA_WIDTH.W)

  val isbr      = Bool()
  val realBrDir = Bool()
  val isCALL    = Bool()
  val isReturn  = Bool()
  val bfail     = new BranchInfo

  val isStore = Bool()

  // exception
  val exc_type    = ECodes()
  val exc_vaddr   = UInt(ADDR_WIDTH.W)
  val isPrivilege = Bool()
  val isException = Bool()

  // csr write info
  val csr_iswf = Bool()
  val isTlb    = Bool()

  // stall info
  val isStall   = Bool()
  val stallType = StallType()

  // debug for chiplab
  // val commitBundle = new RobCommitBundle
}

class RobRenameIO extends Bundle {
  val valid    = Input(Bool())
  val debug_pc = Input(UInt(ADDR_WIDTH.W))
  val index    = Output(UInt(ROB_WIDTH.W))
}

class RobWriteIO extends Bundle {
  val valid = Input(Bool())
  val index = Input(UInt(ROB_WIDTH.W))
  val bits  = Input(new RobInfo)
}

class RobCommitIO extends Bundle {
  val info = DecoupledIO(new RobInfo)
}

// class StoreBufferPipeIO extends Bundle {
//   val from = Flipped(DecoupledIO(new BufferInfo))
//   val to   = DecoupledIO(new BufferInfo)
// }
