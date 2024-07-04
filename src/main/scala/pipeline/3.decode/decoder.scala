package pipeline

import chisel3._
import chisel3.util._

import isa._
import bundles._
import const.ECodes
import const.CSRCodes
import const.Parameters._
import func.Functions._

class DecoderIO extends Bundle {
  val inst = Input(UInt(INST_WIDTH.W))
  val pc   = Input(UInt(ADDR_WIDTH.W))

  val func_type = Output(FuncType())
  val op_type   = Output(UInt(5.W))
  val isload    = Output(Bool())

  val imm = Output(UInt(DATA_WIDTH.W))
  val rj  = Output(UInt(5.W))
  val rk  = Output(UInt(5.W))
  val rd  = Output(UInt(5.W))

  val exc_type = Output(ECodes())
}

class Decoder extends Module {
  val io = IO(new DecoderIO)

  val stable_counter = Module(new StableCounter).io

  io.rj := io.inst(9, 5)
  io.rk := io.inst(14, 10)
  io.rd := io.inst(4, 0)

  val List(func_type, op_type) = ListLookup(io.inst, List(0.U, 0.U), LA32R.table)
  io.func_type := func_type
  io.op_type   := op_type
  io.isload    := func_type === FuncType.mem && MemOpType.isread(op_type)

  val imm05   = io.inst(14, 10)
  val imm12   = SignedExtend(io.inst(21, 10), DATA_WIDTH)
  val imm12u  = UnSignedExtend(io.inst(21, 10), DATA_WIDTH)
  val imm14   = io.inst(23, 10)
  val imm16   = SignedExtend(Cat(io.inst(25, 10), Fill(2, 0.U)), DATA_WIDTH)
  val imm20   = SignedExtend(Cat(io.inst(24, 5), Fill(12, 0.U)), DATA_WIDTH)
  val imm26   = SignedExtend(Cat(io.inst(9, 0), io.inst(25, 10), Fill(2, 0.U)), DATA_WIDTH)
  val use_imm = func_type === FuncType.mem || func_type === FuncType.bru || func_type === FuncType.alu_imm
  val imm = MateDefault(
    func_type,
    0.U,
    Seq(
      FuncType.csr -> imm14,
      FuncType.mem -> imm12,
      FuncType.bru -> Mux(BruOptype.isimm26(op_type), imm26, imm16),
      FuncType.alu_imm -> MuxCase(
        Mux(AluOpType.isimmu(op_type), imm12u, imm12),
        Seq(
          AluOpType.isimm5(op_type)     -> imm05,
          (io.inst === LA32R.LU12I_W)   -> imm20,
          (io.inst === LA32R.PCADDU12I) -> imm20,
        ),
      ),
    ),
  )
  io.imm := imm

  val is_none = func_type === FuncType.none
  val is_exc  = func_type === FuncType.exc

  io.exc_type := MuxCase(
    ECodes.NONE,
    List(
      (is_none && io.pc =/= 0.U)             -> ECodes.INE, // inst does not exist and is not caused by flush
      (is_exc && op_type === ExcOpType.brk)  -> ECodes.BRK,
      (is_exc && op_type === ExcOpType.sys)  -> ECodes.SYS,
      (is_exc && op_type === ExcOpType.ertn) -> ECodes.ertn,
    ),
  )
}