package stages

import chisel3._
import chisel3.util._

import isa._
import bundles._
import const.ECodes
import const.Parameters._
import Funcs.Functions._

class MemoryTopIO extends StageBundle {
  val forward_data  = Output(new ForwardData)
  val forward_tag   = Input(Bool())
  val forward_pc    = Input(UInt(ADDR_WIDTH.W))
  val load_complete = Output(Bool())
  val dCache        = new mem_dCache_IO
}

class MemoryTop extends Module {
  val io   = IO(new MemoryTopIO)
  val busy = WireDefault(false.B)
  val info = StageConnect(io.from, io.to, busy)
  when(io.flush) {
    info        := 0.U.asTypeOf(new info)
    info.bubble := true.B
  }

  val mmu = Module(new Mmu).io
  mmu.func_type                 := info.func_type
  mmu.op_type                   := info.op_type
  mmu.result                    := info.result
  mmu.rd_value                  := info.rd
  io.dCache.request_r.valid     := mmu.data_sram.en && busy && !io.from.fire
  io.dCache.request_r.bits      := mmu.data_sram.addr
  io.dCache.request_w.valid     := mmu.data_sram.we.orR
  io.dCache.request_w.bits.strb := mmu.data_sram.we
  io.dCache.request_w.bits.addr := mmu.data_sram.addr
  io.dCache.request_w.bits.data := mmu.data_sram.wdata
  mmu.data_sram.rdata           := io.dCache.answer.bits
  io.dCache.answer.ready        := true.B
  busy                          := !io.dCache.answer.fire && mmu.data_sram.en

  val has_exc = info.exc_type =/= ECodes.NONE

  val to_info = WireDefault(0.U.asTypeOf(new info))
  to_info           := info
  to_info.isload    := false.B
  to_info.result    := mmu.data
  to_info.exc_type  := Mux(has_exc, info.exc_type, mmu.exc_type)
  to_info.exc_vaddr := Mux(has_exc, info.exc_vaddr, mmu.exc_vaddr)
  to_info.iswf      := Mux(to_info.exc_type =/= ECodes.NONE, false.B, info.iswf)
  when(io.flush) {
    to_info        := 0.U.asTypeOf(new info)
    to_info.bubble := true.B
  }
  io.to.bits := to_info

  io.flush_apply := to_info.exc_type =/= ECodes.NONE && io.to.valid && !info.bubble

  Forward(to_info, io.forward_data)
  io.load_complete := io.dCache.answer.fire && io.forward_tag && info.pc === io.forward_pc
}
/*
因为写后读冲突需要处理ld指令写完寄存器后才能读
但是axi的“慢”，导致原先exe的ld_tag有可能没法传到mem，
解决：选择直接从forwarder连过来
有一个小问题：发现写后读冲突dec级的发现时间，需要早于mem级的写回时间（可以保证吗？）

如果这一个读指令耗时太长，不能让他发出多次询问
解决：在得到答案时就不发出请求了
问题：应该是“在得到答案‘后’就不发出请求了”,但是得到答案就立马流到下一流水级了，所以应该问题不大
问题很大，现在真的遇到这个问题了，原因在于前面一级可能处于某种原因需要等一会，导致这个答案不会立即流
解决：看前面是否fire，如果fire了就不发请求了

load_complete 不应该由 生成tag的前面的指令触发，要对应于发出ld_tag的指令
*/
