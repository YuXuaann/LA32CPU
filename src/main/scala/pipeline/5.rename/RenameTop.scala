package pipeline

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

class RenameTopIO extends StageBundle {
  // rat
  val ratRename = Flipped(Vec(ISSUE_WIDTH, new RatRenameIO))
  val ratRead   = Flipped(Vec(ISSUE_WIDTH, new RatReadIO))
  val ratFull   = Input(Bool())

  // rob
  val rob      = Flipped(Vec(ISSUE_WIDTH, new RobRenameIO))
  val robStall = Input(Bool())
}

class RenameTop extends Module {
  val io = IO(new RenameTopIO)

  val busy = WireDefault(false.B)
  val raw  = stageConnect(io.from, io.to, busy, io.flush)
  when(io.flush) {
    raw._1 := raw._1.getFlushInfo
  }

  val info  = raw._1
  val valid = raw._2
  val res   = WireDefault(info)
  io.to.bits := res

  when(io.ratFull || io.robStall) {
    busy := true.B
  }

  for (i <- 0 until ISSUE_WIDTH) {
    val from = info.bits(i)
    val to   = res.bits(i)

    val writeZero = from.rdInfo.areg === 0.U

    // rename -> rat
    io.ratRename(i).valid := valid && io.to.ready && from.iswf && !writeZero && !io.robStall && from.pipelineType =/= 0.U
    io.ratRename(i).areg  := from.rdInfo.areg
    io.ratRead(i).areg.rj := to.rjInfo.areg
    io.ratRead(i).areg.rk := to.rkInfo.areg

    // rat -> rename
    to.opreg       := Mux(writeZero, 0.U, io.ratRename(i).opreg)
    to.rdInfo.preg := Mux(writeZero, 0.U, io.ratRename(i).preg)
    to.rjInfo.preg := io.ratRead(i).preg.rj
    to.rkInfo.preg := io.ratRead(i).preg.rk
    if (i != 0) {
      for (j <- 0 until i) {
        when(io.ratRename(j).valid) {
          val preRdInfo = io.to.bits.bits(j).rdInfo
          when(preRdInfo.areg === from.rjInfo.areg) {
            to.rjInfo.preg := preRdInfo.preg
          }
          when(preRdInfo.areg === from.rkInfo.areg) {
            to.rkInfo.preg := preRdInfo.preg
          }
          when(preRdInfo.areg === from.rdInfo.areg) {
            to.opreg := preRdInfo.preg
          }
        }
      }
    }

    // rob
    io.rob(i).valid := io.to.ready && valid && from.pipelineType =/= 0.U && !io.ratFull
    to.robId        := io.rob(i).index

    // debug
    io.rob(i).debug_pc := info.bits(i).pc
  }
}
