package cn.komikun;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author ：komikun
 * @date ：Created in 2020-03-11 15,18
 * @description：Mips Assembler
 * @modified By：komikun
 * @version,1.0
 */
public class Assembler {

  public static void main(String[] args) throws IOException {
    String filepath = "src/main/resources/data.asm";
    File file = new File(filepath);
    Program p = new Program(0, new HashMap<String, Integer>());
    String contents = new String(Files.readAllBytes(Paths.get(filepath)));
    p.exeProgram(contents);
  }

}

class Program {

  private int PC;
  private Map<String, Integer> labelMap;

  public Program(int PC, Map<String, Integer> labelMap) {
    this.PC = PC;
    this.labelMap = labelMap;
  }

  public String exeProgram(String str) {
    StringBuilder sb = new StringBuilder();
    String[] strs = str.split("(\r\n|\r|\n)", -1);
    if (!readLabels(strs)){
      return "Wrong Label";
    }
    for (String s : strs) {
      this.PC += 4;
      System.out.println("Start parsing :" + s);
      String[] list = s.split(" ");
      // 0 -> label 1 -> orderName 2 -> para 3 -> comment
      Integer result = 0 ;
      if (list[1] == null){
        return "Empty Order Name";
      }
      if (ROrder.isROrder(list[1])) {
        if (list.length < 3) {
          result = ROrder.transformOrder2Binary(list[1], new ArrayList<String>());
        } else {
          result = ROrder.transformOrder2Binary(list[1], Util.preprocess(list[2]));
        }
      } else if (IOrder.isIOrder(list[1])) {
        System.out.println(list[2]);
        result = IOrder.transformOrder2Binary(list[1], Util.preprocess(list[2]), this.PC, this.labelMap);
      } else if (JOrder.isJOrder(list[1])) {
        result = JOrder.transformOrder2Binary(list[1],Util.preprocess(list[2]),this.labelMap);
      } else {
        result = null;
      }
      if (result != null){
        sb.append(String.format("%08x",result));
        sb.append("\n");
      }else{
        sb.append("No Such Order");
        sb.append("\n");
      }

    }
    System.out.println(sb.toString());
    return sb.toString();
  }

  private Boolean readLabels(String[] str){
    int lineNum = 0;
    for (String s : str) {
      String label = s.split(" ")[0];
      if (!label.equals("")) {
        if (label.endsWith(":")){
          this.labelMap.put(label.substring(0,label.length()-1), lineNum);
        }
        else{
          return false;
        }
      }
      lineNum += 4;
    }
    return true;
  }
}
class Order {

  static Map<String, Integer> registersMap = ImmutableMap.<String, Integer>builder().put("$zero", 0)
      .put("$at", 1).put("$v0", 2).put("$v1", 3).put("$a0", 4).put("$a1", 5).put("$a2", 6)
      .put("$a3", 7).put("$t0", 8).put("$t1", 9).put("$t2", 10).put("$t3", 11).put("$t4", 12)
      .put("$t5", 13).put("$t6", 14).put("$t7", 15).put("$s0", 16).put("$s1", 17).put("$s2", 18)
      .put("$s3", 19).put("$s4", 20).put("$s5", 21).put("$s6", 22).put("$s7", 23).put("$t8", 24)
      .put("$t9", 25).put("$k0", 26).put("$k1", 27).put("$gp", 28).put("$sp", 29).put("$fp", 30)
      .put("$ra", 31).build();

}

class ROrder extends Order {

  private static Set<String> orders = ImmutableSet.<String>builder()
      .add("add", "addu", "sub", "subu", "slt", "sltu", "and", "or", "xor", "nor", "sll", "srl",
          "sllv", "srlv", "srav", "mult", "multu", "div", "divu", "jalr", "eret", "syscall","jr")
      .build();

  public static Boolean isROrder(String orderName) {
    return orders.contains(orderName);
  }

  public static Integer transformOrder2Binary(String orderName, List<String> paraList) {
    String rs, rt, rd;
    switch (paraList.size()) {
      case 0:
        if (orderName.equals("eret")) {
          return (16 << 26) | ((16 & 31) << 21) | (24 & 63);
        } else if (orderName.equals("syscall")) {
          return (0 << 26) | (12 & 63);
        }
        return null;
      case 1:
        rs = paraList.get(0);
        if (orderName.equals("jr")){
          if (registersMap.containsKey(rs)){
            return (0<<26)|(8&63) |(registersMap.get(rs)<<21);
          }else{
            return null;
          }
        }
        return null;
      case 2:
        rs = paraList.get(0);
        rt = paraList.get(1);
        if (!registersMap.containsKey(rs) || !registersMap.containsKey(rt)){
          return null;
        }
        switch (orderName) {
          case "mult":
            return (0 << 26) | (24 & 63) | (registersMap.get(rs) << 21) | (registersMap.get(rt)
                << 16);
          case "multu":
            return (0 << 26) | (25 & 63) | (registersMap.get(rs) << 21) | (registersMap.get(rt)
                << 16);
          case "div":
            return (0 << 26) | (26 & 63) | (registersMap.get(rs) << 21) | (registersMap.get(rt)
                << 16);
          case "divu":
            return (0 << 26) | (27 & 63) | (registersMap.get(rs) << 21) | (registersMap.get(rt)
                << 16);
          case "jalr":
            return (0 << 26) | (9 & 63) | (registersMap.get(rs) << 21) | (registersMap.get(rt)
                << 11);
        }
        return null;
      case 3:
        rs = paraList.get(1);
        rt = paraList.get(2);
        rd = paraList.get(0);
        if (registersMap.containsKey(rs) && registersMap.containsKey(rd)) {
          if (registersMap.containsKey(rt)) {
            switch (orderName) {
              case "add":
                return (0 << 26) | (32 & 63) | (registersMap.get(rs) << 11) | (registersMap.get(rt)
                    << 21) | (registersMap.get(rd) << 16);
              case "addu":
                return (0 << 26) | (33 & 63) | (registersMap.get(rs) << 11) | (registersMap.get(rt)
                    << 21) | (registersMap.get(rd) << 16);
              case "sub":
                return (0 << 26) | (34 & 63) | (registersMap.get(rs) << 11) | (registersMap.get(rt)
                    << 21) | (registersMap.get(rd) << 16);
              case "subu":
                return (0 << 26) | (35 & 63) | (registersMap.get(rs) << 11) | (registersMap.get(rt)
                    << 21) | (registersMap.get(rd) << 16);
              case "slt":
                return (0 << 26) | (42 & 63) | (registersMap.get(rs) << 11) | (registersMap.get(rt)
                    << 21) | (registersMap.get(rd) << 16);
              case "sltu":
                return (0 << 26) | (43 & 63) | (registersMap.get(rs) << 11) | (registersMap.get(rt)
                    << 21) | (registersMap.get(rd) << 16);
              case "and":
                return (0 << 26) | (36 & 63) | (registersMap.get(rs) << 11) | (registersMap.get(rt)
                    << 21) | (registersMap.get(rd) << 16);
              case "or":
                return (0 << 26) | (37 & 63) | (registersMap.get(rs) << 11) | (registersMap.get(rt)
                    << 21) | (registersMap.get(rd) << 16);
              case "xor":
                return (0 << 26) | (38 & 63) | (registersMap.get(rs) << 11) | (registersMap.get(rt)
                    << 21) | (registersMap.get(rd) << 16);
              case "nor":
                return (0 << 26) | (39 & 63) | (registersMap.get(rs) << 11) | (registersMap.get(rt)
                    << 21) | (registersMap.get(rd) << 16);
              case "sllv":
                return (0 << 26) | (4 & 63) | (registersMap.get(rs) << 11) | (registersMap.get(rt)
                    << 21) | (registersMap.get(rd) << 16);
              case "srlv":
                return (0 << 26) | (6 & 63) | (registersMap.get(rs) << 11) | (registersMap.get(rt)
                    << 21) | (registersMap.get(rd) << 16);
              case "srav":
                return (0 << 26) | (7 & 63) | (registersMap.get(rs) << 11) | (registersMap.get(rt)
                    << 21) | (registersMap.get(rd) << 16);
            }
          }else if (Util.isNumber(rt)){
            switch (orderName) {
              case "sll":
                return (0 << 26) | (registersMap.get(rd) << 11) | (registersMap.get(rs) << 21) | (
                    (Util.str2Num(rt) & 31) << 6);
              case "srl":
                return (0 << 26) | (2 & 63) | (registersMap.get(rd) << 11) | (registersMap.get(rs)
                    << 21) | ((Util.str2Num(rt) & 31) << 6);
              case "sra":
                return (0 << 26) | (3 & 63) | (registersMap.get(rd) << 11) | (registersMap.get(rs)
                    << 21) | ((Util.str2Num(rt) & 31) << 6);
            }
          }else{
            return null;
          }

        }
    }
    return null;
  }
}

class IOrder extends Order {

  private static Set<String> twoArgWithBrackets = ImmutableSet.<String>builder()
      .add("lw", "sw", "lh", "lhu", "sh").build();
  private static Set<String> twoArg = ImmutableSet.<String>builder()
      .add("lui", "bgezal").build();
  private static Set<String> threeArg = ImmutableSet.<String>builder()
      .add("addi", "addiu", "andi", "ori", "xori").build();
  private static Set<String> threeArgWithLabel = ImmutableSet.<String>builder()
      .add("beq", "bne").build();
  private static Set<String> orders;

  static {
    Set<String> allTwoArg = Sets.union(twoArg, twoArgWithBrackets);
    Set<String> allThreeArg = Sets.union(threeArg, threeArgWithLabel);
    orders = Sets.union(allThreeArg, allTwoArg);
  }

  public static Boolean isIOrder(String orderName) {
    return orders.contains(orderName);
  }

  public static Integer transformOrder2Binary(String orderName, List<String> paraList, int PC,
      Map<String, Integer> map) {
    if (twoArgWithBrackets.contains(orderName)) {
      String rt = paraList.get(0);
      String tmp = paraList.get(1);
      int start = tmp.indexOf('(');
      int end = tmp.indexOf(')');
      if(start == -1 || end == -1 || end <= start){
        return null;
      }
      String rs = tmp.substring(tmp.indexOf('(') + 1, tmp.indexOf(')'));
      String imm = tmp.substring(0, tmp.indexOf('('));
      if (imm.equals("")){
        imm = "0";
      }
      if (Util.isNumber(imm) && registersMap.containsKey(rt) && registersMap.containsKey(rs)){
        switch (orderName) {
          case "lw":
            return (35 << 26) | (registersMap.get(rt) << 16) | (Util.str2Num(imm) & 0xFFFF) | (
                registersMap.get(rs) << 21);
          case "sw":
            return (43 << 26) | (registersMap.get(rt) << 16) | (Util.str2Num(imm) & 0xFFFF) | (
                registersMap.get(rs) << 21);
          case "lh":
            return (33 << 26) | (registersMap.get(rt) << 16) | (Util.str2Num(imm) & 0xFFFF) | (
                registersMap.get(rs) << 21);
          case "lhu":
            return (37 << 26) | (registersMap.get(rt) << 16) | (Util.str2Num(imm) & 0xFFFF) | (
                registersMap.get(rs) << 21);
          case "sh":
            return (41 << 26) | (registersMap.get(rt) << 16) | (Util.str2Num(imm) & 0xFFFF) | (
                registersMap.get(rs) << 21);
        }
      }else{
        return null;
      }
    }
    if (twoArg.contains(orderName)) {
      String rt = paraList.get(0);
      String imm = paraList.get(1);
      if (registersMap.containsKey(rt) && Util.isNumber(imm)){
        switch (orderName) {
          case "lui":
            return (15 << 26) | (registersMap.get(rt) << 16) | (Util.str2Num(imm) & 0xFFFF);
          case "bgezal":
            return (1 << 26) | ((17 & 31) << 16) | (registersMap.get(rt) << 21) | (
                ((Util.str2Num(imm) - PC - 2) >> 1) & 0xFFFF);
        }
      }else{
        return null;
      }

    }
    if (threeArgWithLabel.contains(orderName)) {
      String rs = paraList.get(0);
      String rt = paraList.get(1);
      String label = paraList.get(2);
      Integer address;
      if (map.containsKey(label)) {
        address = map.get(label);
      } else {
        return null;
      }
      if(registersMap.containsKey(rs) && registersMap.containsKey(rt)){
        switch (orderName) {
          case "beq":
            return (4 << 26) | (registersMap.get(rs) << 21) | (registersMap.get(rt) << 16) | (
                ((address - PC - 2) >> 1) & 0xFFFF);
          case "bne":
            return (5 << 26) | (registersMap.get(rs) << 21) | (registersMap.get(rt) << 16) | (
                ((address - PC - 2) >> 1) & 0xFFFF);
        }
      }else{
        return null;
      }

    }
    if (threeArg.contains(orderName)) {
      String rt = paraList.get(0);
      String rs = paraList.get(1);
      String imm = paraList.get(2);
      if (registersMap.containsKey(rt) && registersMap.containsKey(rs) && Util.isNumber(imm)){
        switch (orderName) {
          case "addi":
            return (8 << 26) | (registersMap.get(rt) << 16) | (registersMap.get(rs) << 21) | (
                Util.str2Num(imm) & 0xFFFF);
          case "addiu":
            return (9 << 26) | (registersMap.get(rt) << 16) | (registersMap.get(rs) << 21) | (
                Util.str2Num(imm) & 0xFFFF);
          case "andi":
            return (12 << 26) | (registersMap.get(rt) << 16) | (registersMap.get(rs) << 21) | (
                Util.str2Num(imm) & 0xFFFF);
          case "ori":
            return (13 << 26) | (registersMap.get(rt) << 16) | (registersMap.get(rs) << 21) | (
                Util.str2Num(imm) & 0xFFFF);
          case "xori":
            return (14 << 26) | (registersMap.get(rt) << 16) | (registersMap.get(rs) << 21) | (
                Util.str2Num(imm) & 0xFFFF);
        }
      }else{
        return null;
      }

    }
    return null;
  }

}
class JOrder extends Order{
  static Set<String> orders = ImmutableSet.<String>builder().add("j","jal").build();
  static Boolean isJOrder(String orderName){
    return orders.contains(orderName);
  }
  static Integer transformOrder2Binary(String orderName , List<String> paraList,Map<String,Integer> map){
    if(orderName.equals("j")){
      String label = paraList.get(0);
      int address;
      if (map.containsKey(label)){
        address = map.get(label);
        return (2<<26) |((address)>>1)&0x3FFFFFF;
      }else if(Util.isNumber(label)){
        return (2<<26) |((Util.str2Num(label))>>1)&0x3FFFFFF;
      }else{
        return null;
      }
    }else if (orderName.equals("jal")){
      if (registersMap.containsKey(paraList.get(0))){
        return (3<<26) |((registersMap.get(paraList.get(0))>>1)&0x3FFFFFF);
      }else{
        return null;
      }
    }
    return null;
  }
}



class Util {
  static Boolean isNumber(String str){
    Pattern pattern = Pattern.compile("-?[0-9]+(\\.[0-9]+)?");
    String bigStr;
    try {
      bigStr = new BigDecimal(str).toString();
    } catch (Exception e) {
      return false;
    }
    Matcher isNum = pattern.matcher(bigStr);
    if (!isNum.matches()) {
      return false;
    }
    return true;
  }

  static void printArray(String[] arr) {
    for (String s : arr) {
      System.out.println(s + ",");
    }
  }

  static int str2Num(String str) {
    return Integer.parseInt(str);
  }

  static List<String> preprocess(String str) {
    List<String> list = new ArrayList<>(Arrays.asList(str.split(",")));
    return list;
  }
}
