package isa

import chisel3._
import chisel3.util._

object LA32R {
  // rdcnt
  def RDCNTID  = BitPat("b0000000000000000011000?????00000")
  def RDCNTVLW = BitPat("b000000000000000001100000000?????")
  def RDCNTVHW = BitPat("b000000000000000001100100000?????")

  // logic reg-reg
  def ADD_W   = BitPat("b00000000000100000???????????????")
  def SUB_W   = BitPat("b00000000000100010???????????????")
  def SLT     = BitPat("b00000000000100100???????????????")
  def SLTU    = BitPat("b00000000000100101???????????????")
  def NOR     = BitPat("b00000000000101000???????????????")
  def AND     = BitPat("b00000000000101001???????????????")
  def OR      = BitPat("b00000000000101010???????????????")
  def XOR     = BitPat("b00000000000101011???????????????")
  def SLL_W   = BitPat("b00000000000101110???????????????")
  def SRL_W   = BitPat("b00000000000101111???????????????")
  def SRA_W   = BitPat("b00000000000110000???????????????")
  def MUL_W   = BitPat("b00000000000111000???????????????")
  def MULH_W  = BitPat("b00000000000111001???????????????")
  def MULH_WU = BitPat("b00000000000111010???????????????")
  def DIV_W   = BitPat("b00000000001000000???????????????")
  def MOD_W   = BitPat("b00000000001000001???????????????")
  def DIV_WU  = BitPat("b00000000001000010???????????????")
  def MOD_WU  = BitPat("b00000000001000011???????????????")

  // else
  def BREAK   = BitPat("b00000000001010100???????????????")
  def SYSCALL = BitPat("b00000000001010110???????????????")

  // logic reg-imm
  def SLLI_W = BitPat("b00000000010000001???????????????")
  def SRLI_W = BitPat("b00000000010001001???????????????")
  def SRAI_W = BitPat("b00000000010010001???????????????")
  def SLTI   = BitPat("b0000001000??????????????????????")
  def SLTUI  = BitPat("b0000001001??????????????????????")
  def ADDI_W = BitPat("b0000001010??????????????????????")
  def ANDI   = BitPat("b0000001101??????????????????????")
  def ORI    = BitPat("b0000001110??????????????????????")
  def XORI   = BitPat("b0000001111??????????????????????")

  // CSR
  def CSRRD   = BitPat("b00000100??????????????00000?????")
  def CSRWR   = BitPat("b00000100??????????????00001?????")
  def CSRXCHG = BitPat("b00000100????????????????????????")

  // cacop
  def CACOP = BitPat("b0000011000??????????????????????") // 尚未实现

  // tlb
  def TLBSRCH = BitPat("b00000110010010000010100000000000") // 尚未实现
  def TLBRD   = BitPat("b00000110010010000010110000000000") // 尚未实现
  def TLBWR   = BitPat("b00000110010010000011000000000000") // 尚未实现
  def TLBFILL = BitPat("b00000110010010000011010000000000") // 尚未实现

  // priv
  def ERTN   = BitPat("b00000110010010000011100000000000")
  def IDLE   = BitPat("b00000110010010001???????????????") // 尚未实现
  def INVTLB = BitPat("b00000110010010011???????????????")

  // imm and pc
  def LU12I_W   = BitPat("b0001010?????????????????????????")
  def PCADDU12I = BitPat("b0001110?????????????????????????")

  // atomtic
  def LL_W = BitPat("b00100000????????????????????????") // 尚未实现
  def SC_W = BitPat("b00100001????????????????????????") // 尚未实现

  // load-store
  def LD_B  = BitPat("b0010100000??????????????????????")
  def LD_H  = BitPat("b0010100001??????????????????????")
  def LD_W  = BitPat("b0010100010??????????????????????")
  def ST_B  = BitPat("b0010100100??????????????????????")
  def ST_H  = BitPat("b0010100101??????????????????????")
  def ST_W  = BitPat("b0010100110??????????????????????")
  def LD_BU = BitPat("b0010101000??????????????????????")
  def LD_HU = BitPat("b0010101001??????????????????????")

  // branch
  def JIRL = BitPat("b010011??????????????????????????")
  def B    = BitPat("b010100??????????????????????????")
  def BL   = BitPat("b010101??????????????????????????")
  def BEQ  = BitPat("b010110??????????????????????????")
  def BNE  = BitPat("b010111??????????????????????????")
  def BLT  = BitPat("b011000??????????????????????????")
  def BGE  = BitPat("b011001??????????????????????????")
  def BLTU = BitPat("b011010??????????????????????????")
  def BGEU = BitPat("b011011??????????????????????????")

  val table = Array(
    // act with stable_counter, then write to rd
    RDCNTID  -> List(FuncType.cnt, CntOpType.cntrd),
    RDCNTVHW -> List(FuncType.cnt, CntOpType.cnth),
    RDCNTVLW -> List(FuncType.cnt, CntOpType.cntl),

    // rj, rk calculated by ALU, then write to rd
    ADD_W   -> List(FuncType.alu, AluOpType.add),
    SUB_W   -> List(FuncType.alu, AluOpType.sub),
    SLT     -> List(FuncType.alu, AluOpType.slt),
    SLTU    -> List(FuncType.alu, AluOpType.sltu),
    NOR     -> List(FuncType.alu, AluOpType.nor),
    AND     -> List(FuncType.alu, AluOpType.and),
    OR      -> List(FuncType.alu, AluOpType.or),
    XOR     -> List(FuncType.alu, AluOpType.xor),
    SLL_W   -> List(FuncType.alu, AluOpType.sll),
    SRL_W   -> List(FuncType.alu, AluOpType.srl),
    SRA_W   -> List(FuncType.alu, AluOpType.sra),
    MUL_W   -> List(FuncType.mul, MulOpType.slow),
    MULH_W  -> List(FuncType.mul, MulOpType.shigh),
    MULH_WU -> List(FuncType.mul, MulOpType.uhigh),
    DIV_W   -> List(FuncType.div, DivOpType.s),
    MOD_W   -> List(FuncType.div, DivOpType.smod),
    DIV_WU  -> List(FuncType.div, DivOpType.u),
    MOD_WU  -> List(FuncType.div, DivOpType.umod),

    // rj, imm(u) calculated by ALU, then write to rd
    LU12I_W   -> List(FuncType.alu_imm, AluOpType.add),
    PCADDU12I -> List(FuncType.alu_imm, AluOpType.add),
    SLLI_W    -> List(FuncType.alu_imm, AluOpType.sll),
    SRLI_W    -> List(FuncType.alu_imm, AluOpType.srl),
    SRAI_W    -> List(FuncType.alu_imm, AluOpType.sra),
    SLTI      -> List(FuncType.alu_imm, AluOpType.slt),
    SLTUI     -> List(FuncType.alu_imm, AluOpType.sltu),
    ADDI_W    -> List(FuncType.alu_imm, AluOpType.add),
    ANDI      -> List(FuncType.alu_imm, AluOpType.and),
    ORI       -> List(FuncType.alu_imm, AluOpType.or),
    XORI      -> List(FuncType.alu_imm, AluOpType.xor),

    // exception
    BREAK   -> List(FuncType.exc, ExcOpType.brk),
    SYSCALL -> List(FuncType.exc, ExcOpType.sys),
    ERTN    -> List(FuncType.exc, ExcOpType.ertn),

    // act with csr and rd
    CSRRD   -> List(FuncType.csr, CsrOpType.rd),
    CSRWR   -> List(FuncType.csr, CsrOpType.wr), // rj as mask
    CSRXCHG -> List(FuncType.csr, CsrOpType.xchg),

    // mem, load and store, address should calculated by ALU.add:rj + imm
    LD_B  -> List(FuncType.mem, MemOpType.readb),
    LD_H  -> List(FuncType.mem, MemOpType.readh),
    LD_W  -> List(FuncType.mem, MemOpType.readw),
    ST_B  -> List(FuncType.mem, MemOpType.writeb),
    ST_H  -> List(FuncType.mem, MemOpType.writeh),
    ST_W  -> List(FuncType.mem, MemOpType.writew),
    LD_BU -> List(FuncType.mem, MemOpType.readbu),
    LD_HU -> List(FuncType.mem, MemOpType.readhu),

    //  bru, jumped address calculated by ALU.add: pc + imm (jirl: rj + imm)
    JIRL -> List(FuncType.bru, BruOptype.jirl),
    B    -> List(FuncType.bru, BruOptype.b),
    BL   -> List(FuncType.bru, BruOptype.bl),
    BEQ  -> List(FuncType.bru, BruOptype.beq),
    BNE  -> List(FuncType.bru, BruOptype.bne),
    BLT  -> List(FuncType.bru, BruOptype.blt),
    BGE  -> List(FuncType.bru, BruOptype.bge),
    BLTU -> List(FuncType.bru, BruOptype.bltu),
    BGEU -> List(FuncType.bru, BruOptype.bgeu),

    // tlb, write, read, and search
    TLBSRCH -> List(FuncType.tlb, TlbOpType.srch),
    TLBRD   -> List(FuncType.tlb, TlbOpType.rd),
    TLBWR   -> List(FuncType.tlb, TlbOpType.wr),
    TLBFILL -> List(FuncType.tlb, TlbOpType.fill),
    INVTLB  -> List(FuncType.tlb, TlbOpType.inv),
  )
}
