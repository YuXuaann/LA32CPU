package stages

import chisel3._
import chisel3.util._

import config._

class rf_bus extends Bundle with Parameters {
  val we    = Bool()
  val wmask = UInt(DATA_WIDTH.W)
  val waddr = UInt(14.W)
  val wdata = UInt(DATA_WIDTH.W)
}

class br_bus extends Bundle with Parameters {
  val br_taken  = Bool()
  val br_target = UInt(ADDR_WIDTH.W)
}

class exc_bus extends Bundle with Parameters {
  val en = Bool()
  val pc = UInt(ADDR_WIDTH.W)
}
