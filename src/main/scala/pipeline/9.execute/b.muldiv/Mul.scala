package pipeline

import chisel3._
import chisel3.util._

import isa._
import const._
import bundles._
import func.Functions._
import const.Parameters._

class SignedMul extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle {
    val CLK = Input(Clock())
    val CE  = Input(Bool())
    val A   = Input(UInt((DATA_WIDTH + 1).W))
    val B   = Input(UInt((DATA_WIDTH + 1).W))
    val P   = Output(UInt((DATA_WIDTH * 2 + 2).W))
  })
}

class MulIO extends Bundle {
  val op_type  = Input(MulOpType())
  val op_type2 = Input(MulOpType())
  val src1     = Input(UInt(DATA_WIDTH.W))
  val src2     = Input(UInt(DATA_WIDTH.W))
  val result   = Output(UInt(DATA_WIDTH.W))
}

// use xilinx ip
// pipeline delay: 2
class Mul extends Module {
  val io = IO(new MulIO)

  if (Config.hasBlackBox) {
    val signed_mul = Module(new SignedMul)
    signed_mul.io.CLK := clock
    signed_mul.io.CE  := true.B
    when(io.op_type === MulOpType.shigh || io.op_type === MulOpType.slow) {
      signed_mul.io.A := Cat(io.src1(DATA_WIDTH - 1), io.src1)
      signed_mul.io.B := Cat(io.src2(DATA_WIDTH - 1), io.src2)
    }.otherwise {
      signed_mul.io.A := Cat(Fill(1, 0.U), io.src1)
      signed_mul.io.B := Cat(Fill(1, 0.U), io.src2)
    }

    io.result := MateDefault(
      io.op_type2,
      0.U,
      List(
        MulOpType.shigh -> signed_mul.io.P(DATA_WIDTH * 2 - 1, DATA_WIDTH),
        MulOpType.slow  -> signed_mul.io.P(DATA_WIDTH, 0),
        MulOpType.uhigh -> signed_mul.io.P(DATA_WIDTH * 2 - 1, DATA_WIDTH),
        MulOpType.ulow  -> signed_mul.io.P(DATA_WIDTH - 1, 0),
      ),
    )
  } else {
    val curSigned       = (io.src1.asSInt * io.src2.asSInt).asUInt
    val curUnsigned     = io.src1 * io.src2
    val signed_result   = ShiftRegister(curSigned, 2)
    val unsigned_result = ShiftRegister(curUnsigned, 2)
    io.result := MateDefault(
      io.op_type2,
      0.U,
      List(
        MulOpType.shigh -> signed_result(DATA_WIDTH * 2 - 1, DATA_WIDTH),
        MulOpType.slow  -> signed_result(DATA_WIDTH, 0),
        MulOpType.uhigh -> unsigned_result(DATA_WIDTH * 2 - 1, DATA_WIDTH),
        MulOpType.ulow  -> unsigned_result(DATA_WIDTH - 1, 0),
      ),
    )
  }
}
