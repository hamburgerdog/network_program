package com.xjosiah.operation;

import java.util.ArrayDeque;
import java.util.ArrayList;

/**
 * 对只含正数的字符串进行简单运算
 * 允许的操作符有：+ - * % ( ) 应当使用英文字符
 * @author xjosiah
 * @since 2020.11.12
 */
public class op_test {

    public static void main(String[] args) {
//        String question = "((1+1.231)*2-1)/123.123-10+293*680";
//        String question = "1*6+2/(2+(3-3))+4+5*2/2";
//        String question = "4/4-1+(1/(1*2)+2*(2-1))";
        String question = "(4/4-1+(1/(1*2)+2*(2-1)+(1023/1023-(1002*1002)/(1002*1002)))";
        ArrayList<String> mathToken = getMathToken(question);
        double result = doMath(mathToken);
        System.out.println(result);
    }

    /**
     * 词法分析：将字符串分析成操作流
     * @param qust  输入的字符串
     * @return      操作流
     */
    private static ArrayList<String> getMathToken(String qust){
        //  用数组存放解析后的字符串，数字和操作符分开处理
        ArrayList<String> token = new ArrayList<>();
        //  先将所有字符都进行分割
        String[] qustSplit = qust.split("");
        //  用于拼接数字字符串
        StringBuilder strTemp=new StringBuilder();
        for (String s:qustSplit){
            switch (s){
                //  操作符不做拼接，直接放入token中
                case "(":
                case "+":
                case "-":
                case "*":
                case "/":
                case ")":
                    //  拼接数字字符串，并放入token
                    //  因为在一个操作符和另一个操作符之间一定只可能是一个数字
                    if (strTemp.length()!=0){
                        token.add(strTemp.toString());
                        strTemp.delete(0,strTemp.length());
                    }
                    token.add(s);
                    break;
                default:
                    strTemp.append(s);
                    break;
            }
        }
        //  对不以')'结尾的字符串需要手动收集最后一个数字
        if (strTemp.length()!=0){
            token.add(strTemp.toString());
            strTemp.delete(0,strTemp.length());
        }
        return token;
    }
    /**
     * 语法语义分析：对token流的进行解析运算，取得运算结果
     * @param token     输入用于操作的流
     * @return          对字符串运算的结果
     */
    private static double doMath(ArrayList<String> token){
        //  存放结果
        double result=0;
        //  运算是使用栈操作实现的，这个就是操作栈
        ArrayDeque mathDeque = new ArrayDeque();
        try {
            //  逐行扫描token流
            for (int i=0;i<token.size();i++){
                String tmp = token.get(i);
                switch (tmp){
                    //  直接放入操作栈
                    case "(":
                        mathDeque.push(tmp);
                        break;
                    //  把'('和')'之中的数据和操作符弹出进行运算，结果返回栈
                    case ")":
                        //  修改token流，减去已执行过的操作
                        token.remove(i);
                        i-=1;
                        for (int j=i;j>=0;j--){
                            if (token.get(j).equals("(")){
                                //  弹出栈顶的数字，此时栈顶只可能是数字，用于运算的临时数据
                                double priTmp=(double)mathDeque.pop();
                                //  弹出操作符，数字后一定带着一个运算符，
                                String s = (String)mathDeque.pop();
                                while(!s.equals("(")) {
                                    //  不需要考虑乘除,因为乘除优先级高，
                                    //  除非操作符后带有'('，否则乘除都会提前弹出运算并返回栈中
                                    if (s.equals("+")) {
                                        priTmp = priTmp + (double) mathDeque.pop();
                                        s = (String)mathDeque.pop();
                                        continue;
                                    }
                                    if (s.equals("-")) {
                                        priTmp = priTmp - (double) mathDeque.pop();
                                        s = (String)mathDeque.pop();
                                        continue;
                                    }
                                }
                                //  对开头使用'('和进行特殊处理，避免异常
                                if (mathDeque.size()==0){
                                    token.remove(j);
                                    token.add(i,String.valueOf(priTmp));
                                    i-=1;
                                    break;
                                }else {
                                    mathDeque.pop();
                                    token.remove(j);
                                    token.add(i,String.valueOf(priTmp));
                                    i-=2;
                                    break;
                                }

                            }
                            else {
                                token.remove(j);
                                i-=1;
                            }
                        }
                        break;
                    case "+":
                        //  如果下一个要读的字符是'('则先将运算符放入栈中，则对其余三个操作符都是一样的
                        if (token.get(i+1).equals("(")){
                            mathDeque.push(tmp);
                            break;
                        }
                        //  如果已经是最后一个操作符了，则直接弹出运行再返回，因为无优先级影响了
                        if (i+2>=token.size()){
                            double dADDTmp = (double)mathDeque.pop() + Double.parseDouble(token.get(i + 1));
                            mathDeque.push(dADDTmp);
                            i+=1;
                            break;
                        }
                        //  如果后续一个操作符优先级和加法相同（即非乘除）就直接弹出运算再放回栈中
                        if (!token.get(i+2).equals("*")&&!token.get(i+2).equals("/")){
                            double dADDTmp = (double)mathDeque.pop() + Double.parseDouble(token.get(i + 1));
                            mathDeque.push(dADDTmp);
                            i+=1;
                        }else mathDeque.push(tmp);
                        break;
                    case "-":
                        if (token.get(i+1).equals("(")){
                            mathDeque.push(tmp);
                            break;
                        }
                        if (i+2>=token.size()){
                            double dADDTmp = (double)mathDeque.pop() - Double.parseDouble(token.get(i + 1));
                            mathDeque.push(dADDTmp);
                            i+=1;
                            break;
                        }
                        if (!token.get(i+2).equals("*")&&!token.get(i+2).equals("/")){
                            double dSUBTmp = (double)mathDeque.pop() - Double.parseDouble(token.get(i + 1));
                            mathDeque.push(dSUBTmp);
                            i+=1;
                        }else mathDeque.push(tmp);
                        break;
                    case "*":
                        if (token.get(i+1).equals("(")){
                            mathDeque.push(tmp);
                            break;
                        }
                        //  在此程序的操作符中乘除优先级是最高，所以直接运算再放入
                        double dMULTmp = (double)mathDeque.pop() * Double.parseDouble(token.get(i+1));
                        mathDeque.push(dMULTmp);
                        i+=1;
                        break;
                    case "/":
                        if (token.get(i+1).equals("(")){
                            mathDeque.push(tmp);
                            break;
                        }
                        double dDIVTmp = (double)mathDeque.pop() / Double.parseDouble(token.get(i+1));
                        mathDeque.push(dDIVTmp);
                        i+=1;
                        break;
                    default:
                        mathDeque.push(Double.parseDouble(tmp));
                        break;
                }
            }
            //  如果操作栈不止一个数据，则说明当前还未完全运算完毕，此时里面存放的都只是无括号和乘除的数据
            //  简单地按从左到右的顺序将所有数据进行统一运算就可以得出结果了
            if (mathDeque.size()!=1){
                //  将栈转成数组更方便操作
                Object[] dequeArray = mathDeque.toArray();
                result=(double)dequeArray[0];
                for (int i=1;i<dequeArray.length;i++){
                    if (dequeArray[i].equals("+")){
                        result = result+(double)dequeArray[i+1];
                        i+=1;
                        continue;
                    }
                    if (dequeArray[i].equals("-")){
                        result = result-(double)dequeArray[i+1];
                        i+=1;
                        continue;
                    }
                }
            }else result=(double)mathDeque.pop();
        }catch (ClassCastException e){
            System.out.println("警告！运算符错误！此程序不支持负数，请重新检查输入的运算符");
            e.printStackTrace();
        }finally {
            return result;
        }
    }
}
