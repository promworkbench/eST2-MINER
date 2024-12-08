package org.processmining.est2miner.algorithms.placeevaluation;

public class MaxFlowPreflowN3 {
    private final int n;
    private final int[][] cap;

    public MaxFlowPreflowN3(int n) {
        this.n = n;
        cap = new int[n][n];
    }

    public void setCapValue(int i, int j, int value) {
        cap[i][j] = value;
    }

    public void setUnbounded(int i, int j) {
        setCapValue(i, j, 20000);
    }

    public int getCapValue(int i, int j) {
        return cap[i][j];
    }

    public int maxFlow(int s, int t) {
        int[] h = new int[n];
        h[s] = this.n - 1;

        int[] maxh = new int[n];
        int[][] f = new int[n][n];

        int[] e = new int[n];

        for (int i = 0; i < n; i++) {
            f[s][i] = cap[s][i];
            f[i][s] = -f[s][i];
            e[i] = cap[s][i];
        }

        for (int sz = 0; ; ) {
            if (sz == 0) {
                for (int i = 0; i < n; i++) {
                    if (i != s && i != t && e[i] > 0) {
                        if (sz != 0 && h[i] > h[maxh[0]]) {
                            sz = 0;
                        }
                        maxh[sz++] = i;
                    }
                }
            }
            if (sz == 0) {
                break;
            }
            while (sz != 0) {
                int i = maxh[sz - 1];
                boolean pushed = false;
                for (int j = 0; j < n && e[i] != 0; j++) {
                    if (h[i] == h[j] + 1 && cap[i][j] - f[i][j] > 0) {
                        int df = Math.min(cap[i][j] - f[i][j], e[i]);
                        f[i][j] += df;
                        f[j][i] -= df;
                        e[i] -= df;
                        e[j] += df;
                        if (e[i] == 0) {
                            sz--;
                        }
                        pushed = true;
                    }
                }
                if (!pushed) {
                    h[i] = 20000;
                    for (int j = 0; j < n; j++) {
                        if (h[i] > h[j] + 1 && cap[i][j] - f[i][j] > 0) {
                            h[i] = h[j] + 1;
                        }
                    }
                    if (h[i] > h[maxh[0]]) {
                        sz = 0;
                        break;
                    }
                }
            }
        }

        int flow = 0;
        for (int i = 0; i < n; i++) {
            flow += f[s][i];
        }

        return flow;
    }
}