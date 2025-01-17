package memory.cache

import chisel3._
import chisel3.util._
import const.Config

class SimpleDualPortRam(RAM_WIDTH: Int, RAM_DEPTH: Int) extends BlackBox(Map("RAM_WIDTH" -> RAM_WIDTH, "RAM_DEPTH" -> RAM_DEPTH)) with HasBlackBoxResource {
  val io = IO(new Bundle {
    val addra = Input(UInt(log2Ceil(RAM_DEPTH).W))
    val clka  = Input(Clock())
    val dina  = Input(UInt(RAM_WIDTH.W))
    val ena   = Input(Bool())
    val wea   = Input(Bool())

    val addrb = Input(UInt(log2Ceil(RAM_DEPTH).W))
    val clkb  = Input(Clock())
    val doutb = Output(UInt(RAM_WIDTH.W))
    val enb   = Input(Bool())
  })
}

class xilinx_simple_dual_port_1_clock_ram_write_first(RAM_WIDTH: Int, RAM_DEPTH: Int)
    extends BlackBox(Map("RAM_WIDTH" -> RAM_WIDTH, "RAM_DEPTH" -> RAM_DEPTH))
    with HasBlackBoxInline {
  val io = IO(new Bundle {
    val addra = Input(UInt(log2Ceil(RAM_DEPTH).W))
    val addrb = Input(UInt(log2Ceil(RAM_DEPTH).W))
    val dina  = Input(UInt(RAM_WIDTH.W))
    val clka  = Input(Clock())
    val wea   = Input(Bool())
    val doutb = Output(UInt(RAM_WIDTH.W))
  })
  val module = "xilinx_simple_dual_port_1_clock_ram_write_first.sv"
  setInline(
    module,
    """
|
|//  Xilinx Simple Dual Port Single Clock RAM
|//  This code implements a parameterizable SDP single clock memory.
|//  If a reset or enable is not necessary, it may be tied off or removed from the code.
|
| module xilinx_simple_dual_port_1_clock_ram_write_first #(
|     parameter RAM_WIDTH = 64,                       // Specify RAM data width
|     parameter RAM_DEPTH = 512                      // Specify RAM depth (number of entries)
|   ) (
|     input  wire [$clog2(RAM_DEPTH)-1:0] addra, // Write address bus, width determined from RAM_DEPTH
|     input  wire [$clog2(RAM_DEPTH)-1:0] addrb, // Read address bus, width determined from RAM_DEPTH
|     input  wire [RAM_WIDTH-1:0] dina,          // RAM input data
|     input  wire clka,                          // Clock
|     input  wire wea,                           // Write enable
|     output wire  [RAM_WIDTH-1:0] doutb         // RAM output data
|   );
|   (*ram_style="block"*)
|     reg [RAM_WIDTH-1:0] BRAM [RAM_DEPTH-1:0];
|     reg [$clog2(RAM_DEPTH)-1:0] addr_r;
|     reg is_collision;
|     reg [RAM_WIDTH-1:0] collison_data;
|
|   generate
|       integer ram_index;
|       initial
|         for (ram_index = 0; ram_index < RAM_DEPTH; ram_index = ram_index + 1)
|           BRAM[ram_index] = {RAM_WIDTH{1'b0}};
|   endgenerate
|
|     always @(posedge clka) begin
|         addr_r <= addrb;
|         is_collision <= (addra == addrb && wea);
|         collison_data <= dina;
|         if (wea) BRAM[addra] <= dina;
|     end
|
|     assign doutb = is_collision == 1'b1 ? collison_data : BRAM[addr_r];
|   endmodule
""".stripMargin,
  )
}

/*

use addrb to read
use addra to write

when same out= bram[same]
when not same
write = bram[addra]
read = bram[addrb]

 */

/*
 val mem = SyncReadMem(RAM_DEPTH, UInt(RAM_WIDTH.W), SyncReadMem.WriteFirst)

    when(io.wea) {
        mem.write(io.addra, io.dina)
    }

    io.doutb := mem.read(io.addrb)
 */
