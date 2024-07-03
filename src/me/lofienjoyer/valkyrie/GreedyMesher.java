package me.lofienjoyer.valkyrie;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// https://github.com/roboleary/GreedyMesh
public class GreedyMesher {

    private final int[] dims;

    private final short[] chunkData;

    private List<Integer> positions;
    private final Random random;

    public GreedyMesher(Chunk chunk, World world) {
        this.chunkData = new short[34 * 34 * 34];
        for (int x = 0; x < 32; x++) {
            for (int y = 0; y < 32; y++) {
                for (int z = 0; z < 32; z++) {
                    chunkData[(x + 1) + (y + 1) * 34 + (z + 1) * (34 * 34)] = chunk.getData()[x | y << 5 | z << 10];
                }
            }
        }

        var minusX = world.getChunk(chunk.getPosition().x - 1, chunk.getPosition().y, chunk.getPosition().z);
        if (minusX != null && minusX.getData() != null) {
            for (int y = 0; y < 32; y++) {
                for (int z = 0; z < 32; z++) {
                    chunkData[0 + (y + 1) * 34 + (z + 1) * (34 * 34)] = minusX.getData()[31 | y << 5 | z << 10];
                }
            }
        }

        var plusX = world.getChunk(chunk.getPosition().x + 1, chunk.getPosition().y, chunk.getPosition().z);
        if (plusX != null && plusX.getData() != null) {
            for (int y = 0; y < 32; y++) {
                for (int z = 0; z < 32; z++) {
                    chunkData[33 + (y + 1) * 34 + (z + 1) * (34 * 34)] = plusX.getData()[0 | y << 5 | z << 10];
                }
            }
        }

        var minusY = world.getChunk(chunk.getPosition().x, chunk.getPosition().y - 1, chunk.getPosition().z);
        if (minusY != null && minusY.getData() != null) {
            for (int x = 0; x < 32; x++) {
                for (int z = 0; z < 32; z++) {
                    chunkData[(x + 1) + 0 + (z + 1) * (34 * 34)] = minusY.getData()[x | 31 << 5 | z << 10];
                }
            }
        }

        var plusY = world.getChunk(chunk.getPosition().x, chunk.getPosition().y + 1, chunk.getPosition().z);
        if (plusY != null && plusY.getData() != null) {
            for (int x = 0; x < 32; x++) {
                for (int z = 0; z < 32; z++) {
                    chunkData[(x + 1) + 33 * 34 + (z + 1) * (34 * 34)] = plusY.getData()[x | 0 | z << 10];
                }
            }
        }

        var minusZ = world.getChunk(chunk.getPosition().x, chunk.getPosition().y, chunk.getPosition().z - 1);
        if (minusZ != null && minusZ.getData() != null) {
            for (int x = 0; x < 32; x++) {
                for (int y = 0; y < 32; y++) {
                    chunkData[(x + 1) + (y + 1) * 34 + 0 * (34 * 34)] = minusZ.getData()[x | y << 5 | 31 << 10];
                }
            }
        }

        var plusZ = world.getChunk(chunk.getPosition().x, chunk.getPosition().y , chunk.getPosition().z + 1);
        if (plusZ != null && plusZ.getData() != null) {
            for (int x = 0; x < 32; x++) {
                for (int y = 0; y < 32; y++) {
                    chunkData[(x + 1) + (y + 1) * 34 + 33 * (34 * 34)] = plusZ.getData()[x | y << 5 | 0];
                }
            }
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
                                    ? backFace ? voxelFace1 | (getLight(x[0], x[1], x[2]) << 4) : voxelFace | (getLight((x[0] + q[0]), (x[1] + q[1]), (x[2] + q[2])) << 4)
                                    : 0;
                        }
                    }

                    x[d]++;

                    n = 0;

                    for(j = 0; j < dims[v]; j++) {

                        for(i = 0; i < dims[u];) {

                            if((mask[n] & 0xf) != 0) {

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
                                        } else {
                                            vertex = getData(x[0], x[1], x[2], 1, w, h);
                                        }
                                    } else if (d == 0) {
                                        if (backFace) {
                                            vertex = getData(x[0], x[1], x[2], 2, h, w);
                                        } else {
                                            vertex = getData(x[0] - 1, x[1], x[2], 3, h, w);
                                        }
                                    } else {
                                        if (!backFace) {
                                            vertex = getData(x[0], x[1] - 1, x[2], 4, h, w);
                                        } else {
                                            vertex = getData(x[0], x[1], x[2], 5, h, w);
                                        }
                                    }

                                    texture = mask[n] - 1;
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
        return chunkData[(x + 1) + (y + 1) * 34 + (z + 1) * (34 * 34)] & 0xf;
    }

    public int getLight(int x, int y, int z) {
        return chunkData[(x + 1) + (y + 1) * 34 + (z + 1) * (34 * 34)] >> 4;
    }

    private static int getData(int x, int y, int z, int face, int width, int height) {
        return face << 18 | z + 1 << 12 | x + 1 << 6 | y + 1 | width - 1 << 21 | height - 1 << 26;
    }

}
