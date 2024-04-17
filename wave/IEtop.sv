// Generated by CIRCT firtool-1.62.0
// Standard header to adapt well known macros for register randomization.
`ifndef RANDOMIZE
  `ifdef RANDOMIZE_REG_INIT
    `define RANDOMIZE
  `endif // RANDOMIZE_REG_INIT
`endif // not def RANDOMIZE

// RANDOM may be set to an expression that produces a 32-bit random unsigned value.
`ifndef RANDOM
  `define RANDOM $random
`endif // not def RANDOM

// Users can define INIT_RANDOM as general code that gets injected into the
// initializer block for modules with registers.
`ifndef INIT_RANDOM
  `define INIT_RANDOM
`endif // not def INIT_RANDOM

// If using random initialization, you can also define RANDOMIZE_DELAY to
// customize the delay used, otherwise 0.002 is used.
`ifndef RANDOMIZE_DELAY
  `define RANDOMIZE_DELAY 0.002
`endif // not def RANDOMIZE_DELAY

// Define INIT_RANDOM_PROLOG_ for use in our modules below.
`ifndef INIT_RANDOM_PROLOG_
  `ifdef RANDOMIZE
    `ifdef VERILATOR
      `define INIT_RANDOM_PROLOG_ `INIT_RANDOM
    `else  // VERILATOR
      `define INIT_RANDOM_PROLOG_ `INIT_RANDOM #`RANDOMIZE_DELAY begin end
    `endif // VERILATOR
  `else  // RANDOMIZE
    `define INIT_RANDOM_PROLOG_
  `endif // RANDOMIZE
`endif // not def INIT_RANDOM_PROLOG_

// Include register initializers in init blocks unless synthesis is set
`ifndef SYNTHESIS
  `ifndef ENABLE_INITIAL_REG_
    `define ENABLE_INITIAL_REG_
  `endif // not def ENABLE_INITIAL_REG_
`endif // not def SYNTHESIS

// Include rmemory initializers in init blocks unless synthesis is set
`ifndef SYNTHESIS
  `ifndef ENABLE_INITIAL_MEM_
    `define ENABLE_INITIAL_MEM_
  `endif // not def ENABLE_INITIAL_MEM_
`endif // not def SYNTHESIS

module Alu(	// src/main/scala/CPU/IEstage/Alu.scala:18:7
  input  [5:0]  io_alu_op,	// src/main/scala/CPU/IEstage/Alu.scala:19:16
  input  [63:0] io_alu_src1,	// src/main/scala/CPU/IEstage/Alu.scala:19:16
                io_alu_src2,	// src/main/scala/CPU/IEstage/Alu.scala:19:16
  output [63:0] io_alu_result	// src/main/scala/CPU/IEstage/Alu.scala:19:16
);

  wire [63:0] _io_alu_result_T_13 = io_alu_src1 | io_alu_src2;	// src/main/scala/CPU/IEstage/Alu.scala:27:45
  wire [94:0] _io_alu_result_T_16 = {31'h0, io_alu_src1} << io_alu_src2[4:0];	// src/main/scala/CPU/IEstage/Alu.scala:30:{56,70}
  wire [63:0] _io_alu_result_T_21 = io_alu_src1 >> io_alu_src2[4:0];	// src/main/scala/CPU/IEstage/Alu.scala:30:70, :31:56
  wire [95:0] _io_alu_result_T_27 =
    {{32{io_alu_src1[31]}}, io_alu_src1} >> io_alu_src2[4:0];	// src/main/scala/CPU/IEstage/Alu.scala:30:70, :32:{34,39,55,75}
  assign io_alu_result =
    io_alu_op == 6'hC
      ? {io_alu_src2[51:0], 12'h0}
      : io_alu_op == 6'hB
          ? {32'h0, _io_alu_result_T_27[31:0]}
          : io_alu_op == 6'hA
              ? {32'h0, _io_alu_result_T_21[31:0]}
              : io_alu_op == 6'h9
                  ? {32'h0, _io_alu_result_T_16[31:0]}
                  : io_alu_op == 6'h8
                      ? io_alu_src1 ^ io_alu_src2
                      : io_alu_op == 6'h7
                          ? _io_alu_result_T_13
                          : io_alu_op == 6'h6
                              ? ~_io_alu_result_T_13
                              : io_alu_op == 6'h5
                                  ? io_alu_src1 & io_alu_src2
                                  : io_alu_op == 6'h4
                                      ? {63'h0, io_alu_src1 < io_alu_src2}
                                      : io_alu_op == 6'h3
                                          ? {63'h0,
                                             $signed(io_alu_src1) < $signed(io_alu_src2)}
                                          : io_alu_op == 6'h2
                                              ? io_alu_src1 - io_alu_src2
                                              : io_alu_op == 6'h1
                                                  ? io_alu_src1 + io_alu_src2
                                                  : 64'h0;	// src/main/scala/CPU/IEstage/Alu.scala:18:7, :22:43, :23:43, :24:54, :25:47, :26:43, :27:{31,45}, :29:43, :30:{56,90}, :31:{56,90}, :32:{39,75,98}, :33:47, src/main/scala/CPU/Parameters/parameters.scala:48:32, :53:27
endmodule

module IEtop(	// src/main/scala/CPU/IEstage/IEtop.scala:23:7
  input         clock,	// src/main/scala/CPU/IEstage/IEtop.scala:23:7
                reset,	// src/main/scala/CPU/IEstage/IEtop.scala:23:7
  output        io_hand_shake_bf_ready_in,	// src/main/scala/CPU/IEstage/IEtop.scala:24:16
  input         io_hand_shake_bf_valid_out,	// src/main/scala/CPU/IEstage/IEtop.scala:24:16
  input  [31:0] io_hand_shake_bf_bus_out_pc,	// src/main/scala/CPU/IEstage/IEtop.scala:24:16
                io_hand_shake_bf_bus_out_inst,	// src/main/scala/CPU/IEstage/IEtop.scala:24:16
  input  [1:0]  io_hand_shake_bf_bus_out_func_type,	// src/main/scala/CPU/IEstage/IEtop.scala:24:16
  input  [9:0]  io_hand_shake_bf_bus_out_op_type,	// src/main/scala/CPU/IEstage/IEtop.scala:24:16
  input  [63:0] io_hand_shake_bf_bus_out_src1,	// src/main/scala/CPU/IEstage/IEtop.scala:24:16
                io_hand_shake_bf_bus_out_src2,	// src/main/scala/CPU/IEstage/IEtop.scala:24:16
                io_hand_shake_bf_bus_out_imm,	// src/main/scala/CPU/IEstage/IEtop.scala:24:16
  input         io_hand_shake_bf_bus_out_rf_bus_valid,	// src/main/scala/CPU/IEstage/IEtop.scala:24:16
  input  [4:0]  io_hand_shake_bf_bus_out_rf_bus_waddr,	// src/main/scala/CPU/IEstage/IEtop.scala:24:16
  input  [63:0] io_hand_shake_bf_bus_out_rf_bus_wdata,	// src/main/scala/CPU/IEstage/IEtop.scala:24:16
                io_hand_shake_bf_bus_out_alu_result,	// src/main/scala/CPU/IEstage/IEtop.scala:24:16
  input         io_hand_shake_af_ready_in,	// src/main/scala/CPU/IEstage/IEtop.scala:24:16
  output        io_hand_shake_af_valid_out,	// src/main/scala/CPU/IEstage/IEtop.scala:24:16
  output [31:0] io_hand_shake_af_bus_out_pc,	// src/main/scala/CPU/IEstage/IEtop.scala:24:16
                io_hand_shake_af_bus_out_inst,	// src/main/scala/CPU/IEstage/IEtop.scala:24:16
  output [1:0]  io_hand_shake_af_bus_out_func_type,	// src/main/scala/CPU/IEstage/IEtop.scala:24:16
  output [9:0]  io_hand_shake_af_bus_out_op_type,	// src/main/scala/CPU/IEstage/IEtop.scala:24:16
  output [63:0] io_hand_shake_af_bus_out_src1,	// src/main/scala/CPU/IEstage/IEtop.scala:24:16
                io_hand_shake_af_bus_out_src2,	// src/main/scala/CPU/IEstage/IEtop.scala:24:16
                io_hand_shake_af_bus_out_imm,	// src/main/scala/CPU/IEstage/IEtop.scala:24:16
  output        io_hand_shake_af_bus_out_rf_bus_valid,	// src/main/scala/CPU/IEstage/IEtop.scala:24:16
  output [4:0]  io_hand_shake_af_bus_out_rf_bus_waddr,	// src/main/scala/CPU/IEstage/IEtop.scala:24:16
  output [63:0] io_hand_shake_af_bus_out_rf_bus_wdata,	// src/main/scala/CPU/IEstage/IEtop.scala:24:16
                io_hand_shake_af_bus_out_alu_result,	// src/main/scala/CPU/IEstage/IEtop.scala:24:16
  output        io_data_sram_en,	// src/main/scala/CPU/IEstage/IEtop.scala:24:16
  output [3:0]  io_data_sram_we,	// src/main/scala/CPU/IEstage/IEtop.scala:24:16
  output [31:0] io_data_sram_addr,	// src/main/scala/CPU/IEstage/IEtop.scala:24:16
                io_data_sram_wdata	// src/main/scala/CPU/IEstage/IEtop.scala:24:16
);

  wire [63:0] _alu_io_alu_result;	// src/main/scala/CPU/IEstage/IEtop.scala:30:21
  reg         bus_valid;	// src/main/scala/CPU/Parameters/parameters.scala:34:28
  wire        io_hand_shake_bf_ready_in_0 = ~bus_valid | io_hand_shake_af_ready_in;	// src/main/scala/CPU/Parameters/parameters.scala:34:28, :36:{23,30}
  wire        _bus_T = io_hand_shake_bf_valid_out & io_hand_shake_bf_ready_in_0;	// src/main/scala/CPU/Parameters/parameters.scala:36:30, :41:27
  wire [63:0] bus_src2 = _bus_T ? io_hand_shake_bf_bus_out_src2 : 64'h0;	// src/main/scala/CPU/Parameters/parameters.scala:33:{27,40}, :41:{27,42}, :42:17
  wire [9:0]  bus_op_type = _bus_T ? io_hand_shake_bf_bus_out_op_type : 10'h0;	// src/main/scala/CPU/Parameters/parameters.scala:33:{27,40}, :41:{27,42}, :42:17
  wire [1:0]  bus_func_type = _bus_T ? io_hand_shake_bf_bus_out_func_type : 2'h0;	// src/main/scala/CPU/Parameters/parameters.scala:33:{27,40}, :41:{27,42}, :42:17
  wire        _GEN = bus_func_type == 2'h1;	// src/main/scala/CPU/IEstage/IEtop.scala:34:25, src/main/scala/CPU/Parameters/parameters.scala:33:27, :41:42, :42:17
  wire        _GEN_0 = _GEN & _bus_T;	// src/main/scala/CPU/IEstage/IEtop.scala:32:21, :34:{25,43}, :36:25, src/main/scala/CPU/Parameters/parameters.scala:41:27
  always @(posedge clock) begin	// src/main/scala/CPU/IEstage/IEtop.scala:23:7
    if (reset)	// src/main/scala/CPU/IEstage/IEtop.scala:23:7
      bus_valid <= 1'h0;	// src/main/scala/CPU/IEstage/IEtop.scala:23:7, src/main/scala/CPU/Parameters/parameters.scala:34:28
    else if (io_hand_shake_bf_ready_in_0)	// src/main/scala/CPU/Parameters/parameters.scala:36:30
      bus_valid <= io_hand_shake_bf_valid_out;	// src/main/scala/CPU/Parameters/parameters.scala:34:28
  end // always @(posedge)
  `ifdef ENABLE_INITIAL_REG_	// src/main/scala/CPU/IEstage/IEtop.scala:23:7
    `ifdef FIRRTL_BEFORE_INITIAL	// src/main/scala/CPU/IEstage/IEtop.scala:23:7
      `FIRRTL_BEFORE_INITIAL	// src/main/scala/CPU/IEstage/IEtop.scala:23:7
    `endif // FIRRTL_BEFORE_INITIAL
    initial begin	// src/main/scala/CPU/IEstage/IEtop.scala:23:7
      automatic logic [31:0] _RANDOM[0:0];	// src/main/scala/CPU/IEstage/IEtop.scala:23:7
      `ifdef INIT_RANDOM_PROLOG_	// src/main/scala/CPU/IEstage/IEtop.scala:23:7
        `INIT_RANDOM_PROLOG_	// src/main/scala/CPU/IEstage/IEtop.scala:23:7
      `endif // INIT_RANDOM_PROLOG_
      `ifdef RANDOMIZE_REG_INIT	// src/main/scala/CPU/IEstage/IEtop.scala:23:7
        _RANDOM[/*Zero width*/ 1'b0] = `RANDOM;	// src/main/scala/CPU/IEstage/IEtop.scala:23:7
        bus_valid = _RANDOM[/*Zero width*/ 1'b0][0];	// src/main/scala/CPU/IEstage/IEtop.scala:23:7, src/main/scala/CPU/Parameters/parameters.scala:34:28
      `endif // RANDOMIZE_REG_INIT
    end // initial
    `ifdef FIRRTL_AFTER_INITIAL	// src/main/scala/CPU/IEstage/IEtop.scala:23:7
      `FIRRTL_AFTER_INITIAL	// src/main/scala/CPU/IEstage/IEtop.scala:23:7
    `endif // FIRRTL_AFTER_INITIAL
  `endif // ENABLE_INITIAL_REG_
  Alu alu (	// src/main/scala/CPU/IEstage/IEtop.scala:30:21
    .io_alu_op     (_GEN ? bus_op_type[5:0] : 6'h3F),	// src/main/scala/CPU/IEstage/IEtop.scala:31:21, :34:{25,43}, :35:23, src/main/scala/CPU/Parameters/parameters.scala:33:27, :41:42, :42:17
    .io_alu_src1   (_GEN_0 ? io_hand_shake_bf_bus_out_src1 : 64'h0),	// src/main/scala/CPU/IEstage/IEtop.scala:32:21, :34:43, :36:25, src/main/scala/CPU/Parameters/parameters.scala:33:40
    .io_alu_src2   (_GEN_0 ? io_hand_shake_bf_bus_out_src2 : 64'h0),	// src/main/scala/CPU/IEstage/IEtop.scala:32:21, :33:21, :34:43, :36:25, :37:25, src/main/scala/CPU/Parameters/parameters.scala:33:40
    .io_alu_result (_alu_io_alu_result)
  );
  assign io_hand_shake_bf_ready_in = io_hand_shake_bf_ready_in_0;	// src/main/scala/CPU/IEstage/IEtop.scala:23:7, src/main/scala/CPU/Parameters/parameters.scala:36:30
  assign io_hand_shake_af_valid_out = bus_valid;	// src/main/scala/CPU/IEstage/IEtop.scala:23:7, src/main/scala/CPU/Parameters/parameters.scala:34:28
  assign io_hand_shake_af_bus_out_pc = _bus_T ? io_hand_shake_bf_bus_out_pc : 32'h0;	// src/main/scala/CPU/IEstage/IEtop.scala:23:7, src/main/scala/CPU/Parameters/parameters.scala:33:{27,40}, :41:{27,42}, :42:17
  assign io_hand_shake_af_bus_out_inst = _bus_T ? io_hand_shake_bf_bus_out_inst : 32'h0;	// src/main/scala/CPU/IEstage/IEtop.scala:23:7, src/main/scala/CPU/Parameters/parameters.scala:33:{27,40}, :41:{27,42}, :42:17
  assign io_hand_shake_af_bus_out_func_type = bus_func_type;	// src/main/scala/CPU/IEstage/IEtop.scala:23:7, src/main/scala/CPU/Parameters/parameters.scala:33:27, :41:42, :42:17
  assign io_hand_shake_af_bus_out_op_type = bus_op_type;	// src/main/scala/CPU/IEstage/IEtop.scala:23:7, src/main/scala/CPU/Parameters/parameters.scala:33:27, :41:42, :42:17
  assign io_hand_shake_af_bus_out_src1 = _bus_T ? io_hand_shake_bf_bus_out_src1 : 64'h0;	// src/main/scala/CPU/IEstage/IEtop.scala:23:7, src/main/scala/CPU/Parameters/parameters.scala:33:{27,40}, :41:{27,42}, :42:17
  assign io_hand_shake_af_bus_out_src2 = bus_src2;	// src/main/scala/CPU/IEstage/IEtop.scala:23:7, src/main/scala/CPU/Parameters/parameters.scala:33:27, :41:42, :42:17
  assign io_hand_shake_af_bus_out_imm = _bus_T ? io_hand_shake_bf_bus_out_imm : 64'h0;	// src/main/scala/CPU/IEstage/IEtop.scala:23:7, src/main/scala/CPU/Parameters/parameters.scala:33:{27,40}, :41:{27,42}, :42:17
  assign io_hand_shake_af_bus_out_rf_bus_valid =
    _bus_T & io_hand_shake_bf_bus_out_rf_bus_valid;	// src/main/scala/CPU/IEstage/IEtop.scala:23:7, src/main/scala/CPU/Parameters/parameters.scala:33:27, :41:{27,42}, :42:17
  assign io_hand_shake_af_bus_out_rf_bus_waddr =
    _bus_T ? io_hand_shake_bf_bus_out_rf_bus_waddr : 5'h0;	// src/main/scala/CPU/IEstage/IEtop.scala:23:7, src/main/scala/CPU/Parameters/parameters.scala:33:{27,40}, :41:{27,42}, :42:17
  assign io_hand_shake_af_bus_out_rf_bus_wdata =
    _bus_T ? io_hand_shake_bf_bus_out_rf_bus_wdata : 64'h0;	// src/main/scala/CPU/IEstage/IEtop.scala:23:7, src/main/scala/CPU/Parameters/parameters.scala:33:{27,40}, :41:{27,42}, :42:17
  assign io_hand_shake_af_bus_out_alu_result = _alu_io_alu_result;	// src/main/scala/CPU/IEstage/IEtop.scala:23:7, :30:21
  assign io_data_sram_en = 1'h1;	// src/main/scala/CPU/IEstage/IEtop.scala:23:7
  assign io_data_sram_we =
    {1'h0,
     bus_func_type == 2'h2 & bus_op_type == 10'h1 & io_hand_shake_af_ready_in
       & io_hand_shake_bf_valid_out,
     2'h0};	// src/main/scala/CPU/IEstage/IEtop.scala:23:7, :45:{21,42}, :46:40, :48:25, src/main/scala/CPU/Parameters/parameters.scala:33:{27,40}, :41:42, :42:17
  assign io_data_sram_addr = _alu_io_alu_result[31:0];	// src/main/scala/CPU/IEstage/IEtop.scala:23:7, :30:21, :49:23
  assign io_data_sram_wdata = bus_src2[31:0];	// src/main/scala/CPU/IEstage/IEtop.scala:23:7, :50:24, src/main/scala/CPU/Parameters/parameters.scala:33:27, :41:42, :42:17
endmodule

