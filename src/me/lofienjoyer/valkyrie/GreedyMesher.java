package me.lofienjoyer.valkyrie;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GreedyMesher {

    private final int[] dims;

    private final byte[] chunkData;

    private List<Integer> positions;
    private final Random random;

    public GreedyMesher(byte[] chunkPreMeshData) {
        this.chunkData = chunkPreMeshData.clone();
        for (int i = 0; i < 6; i++) {
            chunkPreMeshData.clone();
        }

        this.dims = new int[] { 32, 32, 32 };
        this.random = new Random();
    }

    public List<Integer> compute() {
        this.positions = new ArrayList<>(10000);

        computeMesh();

        return positions;
    }

    private void computeMesh() {
        int i, j, k, l, w, h, u, v, n;

        final int[] x = new int []{0,0,0};
        final int[] q = new int []{0,0,0};

        int[] mask;

        int voxelFace, voxelFace1;

        for (boolean backFace = true, b = false; b != backFace; backFace = false, b = !b) {

            for(int d = 0; d < 3; d++) {

                u = (d + 1) % 3;
                v = (d + 2) % 3;

                x[0] = 0;
                x[1] = 0;
                x[2] = 0;

                q[0] = 0;
                q[1] = 0;
                q[2] = 0;
                q[d] = 1;

                mask = new int [(dims[u] + 1) * (dims[v] + 1)];

                for(x[d] = -1; x[d] < dims[d];) {

                    n = 0;

                    for(x[v] = 0; x[v] < dims[v]; x[v]++) {

                        for(x[u] = 0; x[u] < dims[u]; x[u]++) {

                            voxelFace  = getBlock(x[0], x[1], x[2]);
                            voxelFace1 = getBlock((x[0] + q[0]), (x[1] + q[1]), (x[2] + q[2]));

                            mask[n++] = ((voxelFace == 0 || voxelFace1 == 0))
                                    ? backFace ? voxelFace1 : voxelFace
                                    : 0;
                        }
                    }

                    x[d]++;

                    n = 0;

                    for(j = 0; j < dims[v]; j++) {

                        for(i = 0; i < dims[u];) {

                            if(mask[n] != 0) {

                                for(w = 1; i + w < dims[u] && mask[n + w] != 0 && mask[n + w] == mask[n]; w++) {}

                                boolean done = false;

                                for(h = 1; j + h < dims[v]; h++) {

                                    for(k = 0; k < w; k++) {

                                        if(mask[n + k + h * dims[u]] == 0 || mask[n + k + h * dims[u]] != mask[n]) { done = true; break; }
                                    }

                                    if(done) { break; }
                                }

                                if (mask[n] != 0) {
                                    x[u] = i;
                                    x[v] = j;

                                    int vertex;
                                    int texture;

                                    // Texture re-orientation based on the direction
                                    if (d == 2) {
                                        if (!backFace) {
                                            vertex = getData(x[0], x[1], x[2] - 1, 0, w, h);
                                            texture = x[1] / 2;
                                        } else {
                                            vertex = getData(x[0], x[1], x[2], 1, w, h);
                                            texture = x[1] / 2;
                                        }
                                    } else if (d == 0) {
                                        if (backFace) {
                                            vertex = getData(x[0], x[1], x[2], 2, h, w);
                                            texture = x[1] / 2;
                                        } else {
                                            vertex = getData(x[0] - 1, x[1], x[2], 3, h, w);
                                            texture = x[1] / 2;
                                        }
                                    } else {
                                        if (!backFace) {
                                            vertex = getData(x[0], x[1] - 1, x[2], 4, h, w);
                                            texture = x[1] / 2;
                                        } else {
                                            vertex = getData(x[0], x[1], x[2], 5, h, w);
                                            texture = x[1] / 2;
                                        }
                                    }

                                    positions.add(vertex);
                                    positions.add(texture);
                                }

                                for(l = 0; l < h; ++l) {

                                    for(k = 0; k < w; ++k) { mask[n + k + l * dims[u]] = 0; }
                                }

                                i += w;
                                n += w;

                            } else {

                                i++;
                                n++;
                            }
                        }
                    }
                }
            }
        }
    }

    public int getBlock(int x, int y, int z) {
        if (chunkData == null) return 0;
        if (y < 0 || y > 31 || x < 0 || x > 31 || z < 0 || z > 31) return 0;
        return chunkData[x | y << 5 | z << 10];
    }

    private static int getData(int x, int y, int z, int face, int width, int height) {
        return face << 15 | z << 10 | x << 5 | y | width - 1 << 18 | height - 1 << 23;
    }

}
