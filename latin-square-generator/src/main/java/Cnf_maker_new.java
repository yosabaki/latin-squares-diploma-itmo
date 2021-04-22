//программа, генерирующая файл в формате cnf; наивная кодировка усл.орт., 
//amo-предикат определяется попарной кодировкой

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Write a description of class CNF here.
 *
 * @author (your name)
 * @version (a version number or a date)
 */
public class Cnf_maker_new {
    static List<Integer> elements = new ArrayList<Integer>();
    static int c;

    static int y;
    static int c1;
    static int c2;
    static int c3;
    static int c4;
    static int s;
    static int f1;

    //static File f = new File("cnf.cnf");
    static String str;
    static ArrayList<String> st = new ArrayList<String>();

    public static void main(String args[]) throws FileNotFoundException {
        String fileName = "input_beley_new";
        int n = 9;
        int r = 2;
    /* try {
        //проверяем, что если файл не существует то создаем его
        if(!f.exists())f.createNewFile();
    }
        catch(IOException e) {
        throw new RuntimeException(e);
    }
    */

        //столбцы
        for (int w = 0; w < r; w++) {
            for (int i = 0; i < n; i++) {
                for (int t = 0; t < n; t++) {
                    for (int j = 0; j < n; j++) {
                        c = n * n * i + t + n * j + w * n * n * n + 1;
                        elements.add(c);

                    }
                    soch(elements, n);
                    for (int k = n - 1; k >= 0; k--) elements.remove(k);

                }
            }
        }


        //строки
        for (int w = 0; w < r; w++) {
            for (int i = 0; i < n * n; i++) {
                for (int t = 0; t < n; t++) {
                    c = i + n * n * t + w * n * n * n + 1;
                    elements.add(c);

                }
                soch(elements, n);
                for (int k = n - 1; k >= 0; k--) elements.remove(k);
            }
        }
        //элементы
        for (int w = 0; w < r; w++) {
            for (int i = 0; i < n * n; i++) {
                for (int t = 0; t < n; t++) {
                    c = n * i + t + w * n * n * n + 1;
                    elements.add(c);

                }
                soch(elements, n);
                for (int k = n - 1; k >= 0; k--) elements.remove(k);
            }
        }

        //ортогональность
        for (int w = 0; w <= r - 2; w++) {
            for (int j = w + 1; j < r; j++) {
                for (int k = 1; k <= n; k++) {
                    s = n * n - 1;
                    for (int i = 0; i <= n * n - 2; i++) {
                        c1 = k + n * i + w * n * n * n;
                        c2 = k + n * i + j * n * n * n ;

                        for (int t = 1; t <= s; t++) {
                            c3 = k + n * i + n * t + w * n * n * n;
                            c4 = k + n * i + n * t + j * n * n * n;
                            elements.add(c1);
                            elements.add(c2);
                            elements.add(c3);
                            elements.add(c4);
                            soch1(elements);

                        }
                        s--;
                    }
                }
            }
        }

        for (int w = 0; w <= r - 2; w++) {
            for (int l = w + 1; l < r; l++) {
                for (int k = 1; k <= n; k++) {
                    for (int j = 1; j <= n; j++) {
                        if (j != k)
                            s = n * n - 1;
                        for (int i = 0; i <= n * n - 2; i++) {
                            c1 = k + n * i + w * n * n * n;
                            c2 = j + n * i + l * n * n * n;


                            for (int t = 1; t <= s; t++) {
                                c3 = k + n * i + n * t + w * n * n * n;
                                c4 = j + n * i + n * t + l * n * n * n;
                                elements.add(c1);
                                elements.add(c2);
                                elements.add(c3);
                                elements.add(c4);
                                soch1(elements);

                            }

                            s--;
                        }
                    }
                }
            }
        }

        // int a = 3*r*n*n*((n*(n-1)/2)+1)+(n*n*n*n*n*n-n*n*n*n)/2;

        try {
            OutputStream f = new FileOutputStream(fileName, true);
            OutputStreamWriter writer = new OutputStreamWriter(f);
            BufferedWriter out = new BufferedWriter(writer);
            out.write("p cnf " + n * n * n * r + " " + f1 + " ");
            out.write("\n");
            for (int i = 0; i < st.size(); i++) {
                out.write(st.get(i));
                out.flush();
            }

        } catch (IOException ex) {
            System.err.println(ex);
        }
        System.out.print("\n");
    }

    static void soch(List<Integer> e, int y) throws FileNotFoundException {
        for (int i = 0; i < e.size(); i++) st.add(e.get(i) + " ");
        st.add("0 " + "\n");
        f1++;
        for (int i = 0; i <= y - 2; i++) {

            for (int t = i + 1; t < y; t++) {
                st.add("-" + e.get(i) + " -" + e.get(t) + " 0 " + "\n");
                f1++;
            }
            //System.out.println("-"+e.get(i)+" -"+e.get(t)+" 0");
        }

    }


    static void soch1(List<Integer> e) throws FileNotFoundException {

        st.add("-" + e.get(0) + " -" + e.get(2) + " -" + e.get(1) + " -" + e.get(3) + " 0 " + "\n");
        f1++;

        //System.out.println("-"+e.get(0)+" -"+e.get(2)+" -"+e.get(1)+" -"+e.get(3)+" 0");
        for (int i = 3; i >= 0; i--) elements.remove(i);

    }

    public static int soch(int n, int m) {
        int b = factorial(n) / (factorial(m) * factorial(n - m));
        return b;
    }

    public static int factorial(int n) {
        if (n == 0) {
            return 1;
        } else {
            return n * factorial(n - 1);
        }
    }
}