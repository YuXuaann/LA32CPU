package pipeline

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

class DecodeTopIO extends StageBundle {}

class DecodeTop extends Module {
  val io   = IO(new DecodeTopIO)
  val busy = WireDefault(0.U.asTypeOf(new BusyInfo))
  val from = stageConnect(io.from, io.to, busy)

  val info         = from._1.bits
  val valid_signal = from._2
}
