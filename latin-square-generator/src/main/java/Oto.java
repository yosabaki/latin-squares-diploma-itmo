import java.io.*;
import java.util.ArrayList;

public class Oto {
    static ArrayList<String> st = new ArrayList<>();

    static int s1;
    static int s2;
    static int lastVarNumber = 0;

    public static void main(String[] args) {
        s1 = 0;
        s2 = 0;
        String fileName = "input_beley";
        int n = 5;
        int r = 3;
        int log = log2(n);
        int logn2 = log * n * n;
        int[][] matrix = new int[r][logn2];
        for (int k = 0; k < r; k++) {
            for (int i = 0; i < n * n; i++) {
                for (int t = 0; t < log; t++) {
                    matrix[k][i * log + t] = ++lastVarNumber;
                    s1++;
                }
            }
        }

        int[] ceitin = new int[n * n];
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < n; j++) oto(i, j, matrix, ceitin, log, n);
        }


        try {
            OutputStream f = new FileOutputStream(fileName, false);
            OutputStreamWriter writer = new OutputStreamWriter(f);
            BufferedWriter out = new BufferedWriter(writer);
            out.write("p cnf " + s1 + " " + s2 + " ");
            out.write("\n");
            for (int i = 0; i < st.size(); i++) {
                out.write(st.get(i));
                out.flush();
            }
            out.close();

        } catch (IOException ex) {
            System.err.println(ex);
        }
    }

    public static void oto(int q1, int q2, int[][] matrix, int[] ceitin, int log, int n) {
        for (int j = 0; j < n * n; j++) {
            ceitin[j] = ++lastVarNumber;
            s1++;
        }

        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                st.add(ceitin[k * n + i] + " ");
                for (int j = 0; j < log; j++) {
                    if (((q2 >> j) & 1) == 1) {
                        st.add("-" + matrix[q1][k * n * log + i * log + j] + " ");
                    } else {
                        st.add(matrix[q1][k * n * log + i * log + j] + " ");
                    }
                }
                st.add("0 " + "\n");
                s2++;
            }
        }
        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < log; j++) {
                    st.add("-" + ceitin[k * n + i] + " ");
                    if (((q2 >> j) & 1) == 0) {
                        st.add("-" + matrix[q1][k * n * log + i * log + j] + " 0 " + "\n");
                    } else {
                        st.add(matrix[q1][k * n * log + i * log + j] + " 0 " + "\n");
                    }
                    s2++;
                }
            }

        }
        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                st.add(ceitin[k * n + i] + " ");
            }
            st.add("0 " + "\n");
            s2++;
        }
        for (int j = 0; j < n * n; j++) {
            ceitin[j] = ++lastVarNumber;
            s1++;
        }
        for (int i = 0; i < n; i++) {
            for (int k = 0; k < n; k++) {
                st.add(ceitin[k * n + i] + " ");
                for (int j = 0; j < log; j++) {
                    if (((q2 >> j) & 1) == 1) st.add("-" + matrix[q1][k * n * log + i * log + j] + " ");
                    else st.add(matrix[q1][k * n * log + i * log + j] + " ");
                }
                st.add("0 " + "\n");
                s2++;
            }
        }
        for (int i = 0; i < n; i++) {
            for (int k = 0; k < n; k++) {
                for (int j = 0; j < log; j++) {
                    st.add("-" + ceitin[k * n + i] + " ");
                    if (((q2 >> j) & 1) == 0) st.add("-" + matrix[q1][k * n * log + i * log + j] + " 0 " + "\n");
                    else st.add(matrix[q1][k * n * log + i * log + j] + " 0 " + "\n");
                    s2++;
                }
            }
        }
        for (int i = 0; i < n; i++) {
            for (int k = 0; k < n; k++) {
                st.add(ceitin[k * n + i] + " ");
            }
            st.add("0 " + "\n");
            s2++;
        }
    }

    public static int log2(double n) {
        double log1 = Math.log(n) / Math.log(2);
        int s;
        s = (int) Math.ceil(log1);
        return s;

    }
}
