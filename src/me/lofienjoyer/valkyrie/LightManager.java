package me.lofienjoyer.valkyrie;

import java.util.Queue;

public class LightManager {
    
    public static void removeRed(World world, Queue<LightRemovalNode> lightRemovalNodes, Queue<LightNode> lightNodes) {
        while (!lightRemovalNodes.isEmpty()) {
            var node = lightRemovalNodes.poll();
            var pos = node.index();
            var lightLevel = node.val();
            var lightChunk = node.chunk();

            int lightX = pos & 0x1f;
            int lightY = (pos >> 5) & 0x7f;
            int lightZ = pos >> 12;

            if (lightX - 1 < 0) {
                var neighbor = world.getChunk(lightChunk.getPosition().x - 1, lightChunk.getPosition().y);
                int minusXLevel = neighbor.getRedLight(31, lightY, lightZ);
                if (minusXLevel != 0 && minusXLevel < lightLevel) {
                    neighbor.setRedLight(31, lightY, lightZ, 0);
                    int newIndex = 31 | lightY << 5 | lightZ << 12;
                    lightRemovalNodes.add(new LightRemovalNode(newIndex, minusXLevel, neighbor));
                } else {
                    int newIndex = 31 | lightY << 5 | lightZ << 12;
                    lightNodes.add(new LightNode(newIndex, neighbor));
                }
            } else {
                int minusXLevel = lightChunk.getRedLight(lightX - 1, lightY, lightZ);
                if (minusXLevel != 0 && minusXLevel < lightLevel) {
                    lightChunk.setRedLight(lightX - 1, lightY, lightZ, 0);
                    int newIndex = (lightX - 1) | lightY << 5 | lightZ << 12;
                    lightRemovalNodes.add(new LightRemovalNode(newIndex, minusXLevel, lightChunk));
                } else {
                    int newIndex = (lightX - 1) | lightY << 5 | lightZ << 12;
                    lightNodes.add(new LightNode(newIndex, lightChunk));
                }
            }

            if (lightX + 1 > 31) {
                var neighbor = world.getChunk(lightChunk.getPosition().x + 1, lightChunk.getPosition().y);
                int plusXLevel = neighbor.getRedLight(0, lightY, lightZ);
                if (plusXLevel != 0 && plusXLevel < lightLevel) {
                    neighbor.setRedLight(0, lightY, lightZ, 0);
                    int newIndex = 0 | lightY << 5 | lightZ << 12;
                    lightRemovalNodes.add(new LightRemovalNode(newIndex, plusXLevel, neighbor));
                } else {
                    int newIndex = 0 | lightY << 5 | lightZ << 12;
                    lightNodes.add(new LightNode(newIndex, neighbor));
                }
            } else {
                int plusXLevel = lightChunk.getRedLight(lightX + 1, lightY, lightZ);
                if (plusXLevel != 0 && plusXLevel < lightLevel) {
                    lightChunk.setRedLight(lightX + 1, lightY, lightZ, 0);
                    int newIndex = (lightX + 1) | lightY << 5 | lightZ << 12;
                    lightRemovalNodes.add(new LightRemovalNode(newIndex, plusXLevel, lightChunk));
                } else {
                    int newIndex = (lightX + 1) | lightY << 5 | lightZ << 12;
                    lightNodes.add(new LightNode(newIndex, lightChunk));
                }
            }

            if (lightY - 1 < 0) {

            } else {
                int minusYLevel = lightChunk.getRedLight(lightX , lightY - 1, lightZ);
                if (minusYLevel != 0 && minusYLevel < lightLevel) {
                    lightChunk.setRedLight(lightX, lightY - 1, lightZ, 0);
                    int newIndex = lightX | (lightY - 1) << 5 | lightZ << 12;
                    lightRemovalNodes.add(new LightRemovalNode(newIndex, minusYLevel, lightChunk));
                } else {
                    int newIndex = lightX | (lightY - 1) << 5 | lightZ << 12;
                    lightNodes.add(new LightNode(newIndex, lightChunk));
                }
            }

            if (lightY + 1 > 127) {

            } else {
                int plusYLevel = lightChunk.getRedLight(lightX , lightY + 1, lightZ);
                if (plusYLevel != 0 && plusYLevel < lightLevel) {
                    lightChunk.setRedLight(lightX, lightY + 1, lightZ, 0);
                    int newIndex = lightX | (lightY + 1) << 5 | lightZ << 12;
                    lightRemovalNodes.add(new LightRemovalNode(newIndex, plusYLevel, lightChunk));
                } else {
                    int newIndex = lightX | (lightY + 1) << 5 | lightZ << 12;
                    lightNodes.add(new LightNode(newIndex, lightChunk));
                }
            }

            if (lightZ - 1 < 0) {
                var neighbor = world.getChunk(lightChunk.getPosition().x, lightChunk.getPosition().y - 1);
                int minusZLevel = neighbor.getRedLight(lightX, lightY, 31);
                if (minusZLevel != 0 && minusZLevel < lightLevel) {
                    neighbor.setRedLight(lightX, lightY, 31, 0);
                    int newIndex = lightX | lightY << 5 | 31 << 12;
                    lightRemovalNodes.add(new LightRemovalNode(newIndex, minusZLevel, neighbor));
                } else {
                    int newIndex = lightX | lightY << 5 | 31 << 12;
                    lightNodes.add(new LightNode(newIndex, neighbor));
                }
            } else {
                int minusZLevel = lightChunk.getRedLight(lightX , lightY, lightZ - 1);
                if (minusZLevel != 0 && minusZLevel < lightLevel) {
                    lightChunk.setRedLight(lightX, lightY, lightZ - 1, 0);
                    int newIndex = lightX | lightY << 5 | (lightZ - 1) << 12;
                    lightRemovalNodes.add(new LightRemovalNode(newIndex, minusZLevel, lightChunk));
                } else {
                    int newIndex = lightX | lightY << 5 | (lightZ - 1) << 12;
                    lightNodes.add(new LightNode(newIndex, lightChunk));
                }
            }

            if (lightZ + 1 > 31) {
                var neighbor = world.getChunk(lightChunk.getPosition().x, lightChunk.getPosition().y + 1);
                int minusZLevel = neighbor.getRedLight(lightX, lightY, 0);
                if (minusZLevel != 0 && minusZLevel < lightLevel) {
                    neighbor.setRedLight(lightX, lightY, 0, 0);
                    int newIndex = lightX | lightY << 5 | 0 << 12;
                    lightRemovalNodes.add(new LightRemovalNode(newIndex, minusZLevel, neighbor));
                } else {
                    int newIndex = lightX | lightY << 5 | 0 << 12;
                    lightNodes.add(new LightNode(newIndex, neighbor));
                }
            } else {
                int minusZLevel = lightChunk.getRedLight(lightX , lightY, lightZ + 1);
                if (minusZLevel != 0 && minusZLevel < lightLevel) {
                    lightChunk.setRedLight(lightX, lightY, lightZ + 1, 0);
                    int newIndex = lightX | lightY << 5 | (lightZ + 1) << 12;
                    lightRemovalNodes.add(new LightRemovalNode(newIndex, minusZLevel, lightChunk));
                } else {
                    int newIndex = lightX | lightY << 5 | (lightZ + 1) << 12;
                    lightNodes.add(new LightNode(newIndex, lightChunk));
                }
            }
        }
    }

    public static void removeGreen(World world, Queue<LightRemovalNode> lightRemovalNodes, Queue<LightNode> lightNodes) {
        while (!lightRemovalNodes.isEmpty()) {
            var node = lightRemovalNodes.poll();
            var pos = node.index();
            var lightLevel = node.val();
            var lightChunk = node.chunk();

            int lightX = pos & 0x1f;
            int lightY = (pos >> 5) & 0x7f;
            int lightZ = pos >> 12;

            if (lightX - 1 < 0) {
                var neighbor = world.getChunk(lightChunk.getPosition().x - 1, lightChunk.getPosition().y);
                int minusXLevel = neighbor.getGreenLight(31, lightY, lightZ);
                if (minusXLevel != 0 && minusXLevel < lightLevel) {
                    neighbor.setGreenLight(31, lightY, lightZ, 0);
                    int newIndex = 31 | lightY << 5 | lightZ << 12;
                    lightRemovalNodes.add(new LightRemovalNode(newIndex, minusXLevel, neighbor));
                } else {
                    int newIndex = 31 | lightY << 5 | lightZ << 12;
                    lightNodes.add(new LightNode(newIndex, neighbor));
                }
            } else {
                int minusXLevel = lightChunk.getGreenLight(lightX - 1, lightY, lightZ);
                if (minusXLevel != 0 && minusXLevel < lightLevel) {
                    lightChunk.setGreenLight(lightX - 1, lightY, lightZ, 0);
                    int newIndex = (lightX - 1) | lightY << 5 | lightZ << 12;
                    lightRemovalNodes.add(new LightRemovalNode(newIndex, minusXLevel, lightChunk));
                } else {
                    int newIndex = (lightX - 1) | lightY << 5 | lightZ << 12;
                    lightNodes.add(new LightNode(newIndex, lightChunk));
                }
            }

            if (lightX + 1 > 31) {
                var neighbor = world.getChunk(lightChunk.getPosition().x + 1, lightChunk.getPosition().y);
                int plusXLevel = neighbor.getGreenLight(0, lightY, lightZ);
                if (plusXLevel != 0 && plusXLevel < lightLevel) {
                    neighbor.setGreenLight(0, lightY, lightZ, 0);
                    int newIndex = 0 | lightY << 5 | lightZ << 12;
                    lightRemovalNodes.add(new LightRemovalNode(newIndex, plusXLevel, neighbor));
                } else {
                    int newIndex = 0 | lightY << 5 | lightZ << 12;
                    lightNodes.add(new LightNode(newIndex, neighbor));
                }
            } else {
                int plusXLevel = lightChunk.getGreenLight(lightX + 1, lightY, lightZ);
                if (plusXLevel != 0 && plusXLevel < lightLevel) {
                    lightChunk.setGreenLight(lightX + 1, lightY, lightZ, 0);
                    int newIndex = (lightX + 1) | lightY << 5 | lightZ << 12;
                    lightRemovalNodes.add(new LightRemovalNode(newIndex, plusXLevel, lightChunk));
                } else {
                    int newIndex = (lightX + 1) | lightY << 5 | lightZ << 12;
                    lightNodes.add(new LightNode(newIndex, lightChunk));
                }
            }

            if (lightY - 1 < 0) {

            } else {
                int minusYLevel = lightChunk.getGreenLight(lightX , lightY - 1, lightZ);
                if (minusYLevel != 0 && minusYLevel < lightLevel) {
                    lightChunk.setGreenLight(lightX, lightY - 1, lightZ, 0);
                    int newIndex = lightX | (lightY - 1) << 5 | lightZ << 12;
                    lightRemovalNodes.add(new LightRemovalNode(newIndex, minusYLevel, lightChunk));
                } else {
                    int newIndex = lightX | (lightY - 1) << 5 | lightZ << 12;
                    lightNodes.add(new LightNode(newIndex, lightChunk));
                }
            }

            if (lightY + 1 > 127) {

            } else {
                int plusYLevel = lightChunk.getGreenLight(lightX , lightY + 1, lightZ);
                if (plusYLevel != 0 && plusYLevel < lightLevel) {
                    lightChunk.setGreenLight(lightX, lightY + 1, lightZ, 0);
                    int newIndex = lightX | (lightY + 1) << 5 | lightZ << 12;
                    lightRemovalNodes.add(new LightRemovalNode(newIndex, plusYLevel, lightChunk));
                } else {
                    int newIndex = lightX | (lightY + 1) << 5 | lightZ << 12;
                    lightNodes.add(new LightNode(newIndex, lightChunk));
                }
            }

            if (lightZ - 1 < 0) {
                var neighbor = world.getChunk(lightChunk.getPosition().x, lightChunk.getPosition().y - 1);
                int minusZLevel = neighbor.getGreenLight(lightX, lightY, 31);
                if (minusZLevel != 0 && minusZLevel < lightLevel) {
                    neighbor.setGreenLight(lightX, lightY, 31, 0);
                    int newIndex = lightX | lightY << 5 | 31 << 12;
                    lightRemovalNodes.add(new LightRemovalNode(newIndex, minusZLevel, neighbor));
                } else {
                    int newIndex = lightX | lightY << 5 | 31 << 12;
                    lightNodes.add(new LightNode(newIndex, neighbor));
                }
            } else {
                int minusZLevel = lightChunk.getGreenLight(lightX , lightY, lightZ - 1);
                if (minusZLevel != 0 && minusZLevel < lightLevel) {
                    lightChunk.setGreenLight(lightX, lightY, lightZ - 1, 0);
                    int newIndex = lightX | lightY << 5 | (lightZ - 1) << 12;
                    lightRemovalNodes.add(new LightRemovalNode(newIndex, minusZLevel, lightChunk));
                } else {
                    int newIndex = lightX | lightY << 5 | (lightZ - 1) << 12;
                    lightNodes.add(new LightNode(newIndex, lightChunk));
                }
            }

            if (lightZ + 1 > 31) {
                var neighbor = world.getChunk(lightChunk.getPosition().x, lightChunk.getPosition().y + 1);
                int minusZLevel = neighbor.getGreenLight(lightX, lightY, 0);
                if (minusZLevel != 0 && minusZLevel < lightLevel) {
                    neighbor.setGreenLight(lightX, lightY, 0, 0);
                    int newIndex = lightX | lightY << 5 | 0 << 12;
                    lightRemovalNodes.add(new LightRemovalNode(newIndex, minusZLevel, neighbor));
                } else {
                    int newIndex = lightX | lightY << 5 | 0 << 12;
                    lightNodes.add(new LightNode(newIndex, neighbor));
                }
            } else {
                int minusZLevel = lightChunk.getGreenLight(lightX , lightY, lightZ + 1);
                if (minusZLevel != 0 && minusZLevel < lightLevel) {
                    lightChunk.setGreenLight(lightX, lightY, lightZ + 1, 0);
                    int newIndex = lightX | lightY << 5 | (lightZ + 1) << 12;
                    lightRemovalNodes.add(new LightRemovalNode(newIndex, minusZLevel, lightChunk));
                } else {
                    int newIndex = lightX | lightY << 5 | (lightZ + 1) << 12;
                    lightNodes.add(new LightNode(newIndex, lightChunk));
                }
            }
        }
    }

    public static void removeBlue(World world, Queue<LightRemovalNode> lightRemovalNodes, Queue<LightNode> lightNodes) {
        while (!lightRemovalNodes.isEmpty()) {
            var node = lightRemovalNodes.poll();
            var pos = node.index();
            var lightLevel = node.val();
            var lightChunk = node.chunk();

            int lightX = pos & 0x1f;
            int lightY = (pos >> 5) & 0x7f;
            int lightZ = pos >> 12;

            if (lightX - 1 < 0) {
                var neighbor = world.getChunk(lightChunk.getPosition().x - 1, lightChunk.getPosition().y);
                int minusXLevel = neighbor.getBlueLight(31, lightY, lightZ);
                if (minusXLevel != 0 && minusXLevel < lightLevel) {
                    neighbor.setBlueLight(31, lightY, lightZ, 0);
                    int newIndex = 31 | lightY << 5 | lightZ << 12;
                    lightRemovalNodes.add(new LightRemovalNode(newIndex, minusXLevel, neighbor));
                } else {
                    int newIndex = 31 | lightY << 5 | lightZ << 12;
                    lightNodes.add(new LightNode(newIndex, neighbor));
                }
            } else {
                int minusXLevel = lightChunk.getBlueLight(lightX - 1, lightY, lightZ);
                if (minusXLevel != 0 && minusXLevel < lightLevel) {
                    lightChunk.setBlueLight(lightX - 1, lightY, lightZ, 0);
                    int newIndex = (lightX - 1) | lightY << 5 | lightZ << 12;
                    lightRemovalNodes.add(new LightRemovalNode(newIndex, minusXLevel, lightChunk));
                } else {
                    int newIndex = (lightX - 1) | lightY << 5 | lightZ << 12;
                    lightNodes.add(new LightNode(newIndex, lightChunk));
                }
            }

            if (lightX + 1 > 31) {
                var neighbor = world.getChunk(lightChunk.getPosition().x + 1, lightChunk.getPosition().y);
                int plusXLevel = neighbor.getBlueLight(0, lightY, lightZ);
                if (plusXLevel != 0 && plusXLevel < lightLevel) {
                    neighbor.setBlueLight(0, lightY, lightZ, 0);
                    int newIndex = 0 | lightY << 5 | lightZ << 12;
                    lightRemovalNodes.add(new LightRemovalNode(newIndex, plusXLevel, neighbor));
                } else {
                    int newIndex = 0 | lightY << 5 | lightZ << 12;
                    lightNodes.add(new LightNode(newIndex, neighbor));
                }
            } else {
                int plusXLevel = lightChunk.getBlueLight(lightX + 1, lightY, lightZ);
                if (plusXLevel != 0 && plusXLevel < lightLevel) {
                    lightChunk.setBlueLight(lightX + 1, lightY, lightZ, 0);
                    int newIndex = (lightX + 1) | lightY << 5 | lightZ << 12;
                    lightRemovalNodes.add(new LightRemovalNode(newIndex, plusXLevel, lightChunk));
                } else {
                    int newIndex = (lightX + 1) | lightY << 5 | lightZ << 12;
                    lightNodes.add(new LightNode(newIndex, lightChunk));
                }
            }

            if (lightY - 1 < 0) {

            } else {
                int minusYLevel = lightChunk.getBlueLight(lightX , lightY - 1, lightZ);
                if (minusYLevel != 0 && minusYLevel < lightLevel) {
                    lightChunk.setBlueLight(lightX, lightY - 1, lightZ, 0);
                    int newIndex = lightX | (lightY - 1) << 5 | lightZ << 12;
                    lightRemovalNodes.add(new LightRemovalNode(newIndex, minusYLevel, lightChunk));
                } else {
                    int newIndex = lightX | (lightY - 1) << 5 | lightZ << 12;
                    lightNodes.add(new LightNode(newIndex, lightChunk));
                }
            }

            if (lightY + 1 > 127) {

            } else {
                int plusYLevel = lightChunk.getBlueLight(lightX , lightY + 1, lightZ);
                if (plusYLevel != 0 && plusYLevel < lightLevel) {
                    lightChunk.setBlueLight(lightX, lightY + 1, lightZ, 0);
                    int newIndex = lightX | (lightY + 1) << 5 | lightZ << 12;
                    lightRemovalNodes.add(new LightRemovalNode(newIndex, plusYLevel, lightChunk));
                } else {
                    int newIndex = lightX | (lightY + 1) << 5 | lightZ << 12;
                    lightNodes.add(new LightNode(newIndex, lightChunk));
                }
            }

            if (lightZ - 1 < 0) {
                var neighbor = world.getChunk(lightChunk.getPosition().x, lightChunk.getPosition().y - 1);
                int minusZLevel = neighbor.getBlueLight(lightX, lightY, 31);
                if (minusZLevel != 0 && minusZLevel < lightLevel) {
                    neighbor.setBlueLight(lightX, lightY, 31, 0);
                    int newIndex = lightX | lightY << 5 | 31 << 12;
                    lightRemovalNodes.add(new LightRemovalNode(newIndex, minusZLevel, neighbor));
                } else {
                    int newIndex = lightX | lightY << 5 | 31 << 12;
                    lightNodes.add(new LightNode(newIndex, neighbor));
                }
            } else {
                int minusZLevel = lightChunk.getBlueLight(lightX , lightY, lightZ - 1);
                if (minusZLevel != 0 && minusZLevel < lightLevel) {
                    lightChunk.setBlueLight(lightX, lightY, lightZ - 1, 0);
                    int newIndex = lightX | lightY << 5 | (lightZ - 1) << 12;
                    lightRemovalNodes.add(new LightRemovalNode(newIndex, minusZLevel, lightChunk));
                } else {
                    int newIndex = lightX | lightY << 5 | (lightZ - 1) << 12;
                    lightNodes.add(new LightNode(newIndex, lightChunk));
                }
            }

            if (lightZ + 1 > 31) {
                var neighbor = world.getChunk(lightChunk.getPosition().x, lightChunk.getPosition().y + 1);
                int minusZLevel = neighbor.getBlueLight(lightX, lightY, 0);
                if (minusZLevel != 0 && minusZLevel < lightLevel) {
                    neighbor.setBlueLight(lightX, lightY, 0, 0);
                    int newIndex = lightX | lightY << 5 | 0 << 12;
                    lightRemovalNodes.add(new LightRemovalNode(newIndex, minusZLevel, neighbor));
                } else {
                    int newIndex = lightX | lightY << 5 | 0 << 12;
                    lightNodes.add(new LightNode(newIndex, neighbor));
                }
            } else {
                int minusZLevel = lightChunk.getBlueLight(lightX , lightY, lightZ + 1);
                if (minusZLevel != 0 && minusZLevel < lightLevel) {
                    lightChunk.setBlueLight(lightX, lightY, lightZ + 1, 0);
                    int newIndex = lightX | lightY << 5 | (lightZ + 1) << 12;
                    lightRemovalNodes.add(new LightRemovalNode(newIndex, minusZLevel, lightChunk));
                } else {
                    int newIndex = lightX | lightY << 5 | (lightZ + 1) << 12;
                    lightNodes.add(new LightNode(newIndex, lightChunk));
                }
            }
        }
    }

    public static void removeSun(World world, Queue<LightRemovalNode> lightRemovalNodes, Queue<LightNode> lightNodes) {
        while (!lightRemovalNodes.isEmpty()) {
            var node = lightRemovalNodes.poll();
            var pos = node.index();
            var lightLevel = node.val();
            var lightChunk = node.chunk();

            int lightX = pos & 0x1f;
            int lightY = (pos >> 5) & 0x7f;
            int lightZ = pos >> 12;

            if (lightX - 1 < 0) {
                var neighbor = world.getChunk(lightChunk.getPosition().x - 1, lightChunk.getPosition().y);
                if (neighbor != null) {
                    int minusXLevel = neighbor.getSunLight(31, lightY, lightZ);
                    if (minusXLevel != 0 && minusXLevel < lightLevel) {
                        neighbor.setSunLight(31, lightY, lightZ, 0);
                        int newIndex = 31 | lightY << 5 | lightZ << 12;
                        lightRemovalNodes.add(new LightRemovalNode(newIndex, minusXLevel, neighbor));
                    } else {
                        int newIndex = 31 | lightY << 5 | lightZ << 12;
                        lightNodes.add(new LightNode(newIndex, neighbor));
                    }
                }
            } else {
                int minusXLevel = lightChunk.getSunLight(lightX - 1, lightY, lightZ);
                if (minusXLevel != 0 && minusXLevel < lightLevel) {
                    lightChunk.setSunLight(lightX - 1, lightY, lightZ, 0);
                    int newIndex = (lightX - 1) | lightY << 5 | lightZ << 12;
                    lightRemovalNodes.add(new LightRemovalNode(newIndex, minusXLevel, lightChunk));
                } else {
                    int newIndex = (lightX - 1) | lightY << 5 | lightZ << 12;
                    lightNodes.add(new LightNode(newIndex, lightChunk));
                }
            }

            if (lightX + 1 > 31) {
                var neighbor = world.getChunk(lightChunk.getPosition().x + 1, lightChunk.getPosition().y);
                if (neighbor != null) {
                    int plusXLevel = neighbor.getSunLight(0, lightY, lightZ);
                    if (plusXLevel != 0 && plusXLevel < lightLevel) {
                        neighbor.setSunLight(0, lightY, lightZ, 0);
                        int newIndex = 0 | lightY << 5 | lightZ << 12;
                        lightRemovalNodes.add(new LightRemovalNode(newIndex, plusXLevel, neighbor));
                    } else {
                        int newIndex = 0 | lightY << 5 | lightZ << 12;
                        lightNodes.add(new LightNode(newIndex, neighbor));
                    }
                }
            } else {
                int plusXLevel = lightChunk.getSunLight(lightX + 1, lightY, lightZ);
                if (plusXLevel != 0 && plusXLevel < lightLevel) {
                    lightChunk.setSunLight(lightX + 1, lightY, lightZ, 0);
                    int newIndex = (lightX + 1) | lightY << 5 | lightZ << 12;
                    lightRemovalNodes.add(new LightRemovalNode(newIndex, plusXLevel, lightChunk));
                } else {
                    int newIndex = (lightX + 1) | lightY << 5 | lightZ << 12;
                    lightNodes.add(new LightNode(newIndex, lightChunk));
                }
            }

            if (lightY - 1 < 0) {

            } else {
                int minusYLevel = lightChunk.getSunLight(lightX , lightY - 1, lightZ);
                if (minusYLevel != 0 && minusYLevel < lightLevel) {
                    lightChunk.setSunLight(lightX, lightY - 1, lightZ, 0);
                    int newIndex = lightX | (lightY - 1) << 5 | lightZ << 12;
                    lightRemovalNodes.add(new LightRemovalNode(newIndex, minusYLevel, lightChunk));
                } else {
                    int newIndex = lightX | (lightY - 1) << 5 | lightZ << 12;
                    lightNodes.add(new LightNode(newIndex, lightChunk));
                }
            }

            if (lightY + 1 > 127) {

            } else {
                int plusYLevel = lightChunk.getSunLight(lightX , lightY + 1, lightZ);
                if (plusYLevel != 0 && plusYLevel < lightLevel) {
                    lightChunk.setSunLight(lightX, lightY + 1, lightZ, 0);
                    int newIndex = lightX | (lightY + 1) << 5 | lightZ << 12;
                    lightRemovalNodes.add(new LightRemovalNode(newIndex, plusYLevel, lightChunk));
                } else {
                    int newIndex = lightX | (lightY + 1) << 5 | lightZ << 12;
                    lightNodes.add(new LightNode(newIndex, lightChunk));
                }
            }

            if (lightZ - 1 < 0) {
                var neighbor = world.getChunk(lightChunk.getPosition().x, lightChunk.getPosition().y - 1);
                if (neighbor != null) {
                    int minusZLevel = neighbor.getSunLight(lightX, lightY, 31);
                    if (minusZLevel != 0 && minusZLevel < lightLevel) {
                        neighbor.setSunLight(lightX, lightY, 31, 0);
                        int newIndex = lightX | lightY << 5 | 31 << 12;
                        lightRemovalNodes.add(new LightRemovalNode(newIndex, minusZLevel, neighbor));
                    } else {
                        int newIndex = lightX | lightY << 5 | 31 << 12;
                        lightNodes.add(new LightNode(newIndex, neighbor));
                    }
                }
            } else {
                int minusZLevel = lightChunk.getSunLight(lightX , lightY, lightZ - 1);
                if (minusZLevel != 0 && minusZLevel < lightLevel) {
                    lightChunk.setSunLight(lightX, lightY, lightZ - 1, 0);
                    int newIndex = lightX | lightY << 5 | (lightZ - 1) << 12;
                    lightRemovalNodes.add(new LightRemovalNode(newIndex, minusZLevel, lightChunk));
                } else {
                    int newIndex = lightX | lightY << 5 | (lightZ - 1) << 12;
                    lightNodes.add(new LightNode(newIndex, lightChunk));
                }
            }

            if (lightZ + 1 > 31) {
                var neighbor = world.getChunk(lightChunk.getPosition().x, lightChunk.getPosition().y + 1);
                if (neighbor != null) {
                    int minusZLevel = neighbor.getSunLight(lightX, lightY, 0);
                    if (minusZLevel != 0 && minusZLevel < lightLevel) {
                        neighbor.setSunLight(lightX, lightY, 0, 0);
                        int newIndex = lightX | lightY << 5 | 0 << 12;
                        lightRemovalNodes.add(new LightRemovalNode(newIndex, minusZLevel, neighbor));
                    } else {
                        int newIndex = lightX | lightY << 5 | 0 << 12;
                        lightNodes.add(new LightNode(newIndex, neighbor));
                    }
                }
            } else {
                int minusZLevel = lightChunk.getSunLight(lightX , lightY, lightZ + 1);
                if (minusZLevel != 0 && minusZLevel < lightLevel) {
                    lightChunk.setSunLight(lightX, lightY, lightZ + 1, 0);
                    int newIndex = lightX | lightY << 5 | (lightZ + 1) << 12;
                    lightRemovalNodes.add(new LightRemovalNode(newIndex, minusZLevel, lightChunk));
                } else {
                    int newIndex = lightX | lightY << 5 | (lightZ + 1) << 12;
                    lightNodes.add(new LightNode(newIndex, lightChunk));
                }
            }
        }
    }

    public static void propagateRed(World world, Queue<LightNode> lightNodes) {
        while (!lightNodes.isEmpty()) {
            var node = lightNodes.poll();

            int pos = node.position();
            var lightChunk = node.chunk();

            int lightX = pos & 0x1f;
            int lightY = (pos >> 5) & 0x7f;
            int lightZ = pos >> 12;
            int lightLevel = lightChunk.getRedLight(lightX, lightY, lightZ);

            if (lightX - 1 < 0) {
                var neighbor = world.getChunk(lightChunk.getPosition().x - 1, lightChunk.getPosition().y);
                if (neighbor.getBlock(lightX - 1 + 32, lightY, lightZ) == 0 && neighbor.getRedLight(lightX - 1 + 32, lightY, lightZ) + 2 <= lightLevel) {
                    neighbor.setRedLight(lightX - 1 + 32, lightY, lightZ, lightLevel - 1);
                    int newIndex = 31 | lightY << 5 | lightZ << 12;
                    lightNodes.add(new LightNode(newIndex, neighbor));
                }
            } else {
                if (lightChunk.getBlock(lightX - 1, lightY, lightZ) == 0 && lightChunk.getRedLight(lightX - 1, lightY, lightZ) + 2 <= lightLevel) {
                    lightChunk.setRedLight(lightX - 1, lightY, lightZ, lightLevel - 1);
                    int newIndex = (lightX - 1) | lightY << 5 | lightZ << 12;
                    lightNodes.add(new LightNode(newIndex, lightChunk));
                }
            }

            if (lightX + 1 > 31) {
                var neighbor = world.getChunk(lightChunk.getPosition().x + 1, lightChunk.getPosition().y);
                if (neighbor.getBlock(0, lightY, lightZ) == 0 && neighbor.getRedLight(0, lightY, lightZ) + 2 <= lightLevel) {
                    neighbor.setRedLight(0, lightY, lightZ, lightLevel - 1);
                    int newIndex = 0 | lightY << 5 | lightZ << 12;
                    lightNodes.add(new LightNode(newIndex, neighbor));
                }
            } else {
                if (lightChunk.getBlock(lightX + 1, lightY, lightZ) == 0 && lightChunk.getRedLight(lightX + 1, lightY, lightZ) + 2 <= lightLevel) {
                    lightChunk.setRedLight(lightX + 1, lightY, lightZ, lightLevel - 1);
                    int newIndex = (lightX + 1) | lightY << 5 | lightZ << 12;
                    lightNodes.add(new LightNode(newIndex, lightChunk));
                }
            }

            if (lightY - 1 < 0) {

            } else {
                if (lightChunk.getBlock(lightX, lightY - 1, lightZ) == 0 && lightChunk.getRedLight(lightX, lightY - 1, lightZ) + 2 <= lightLevel) {
                    lightChunk.setRedLight(lightX, lightY - 1, lightZ, lightLevel - 1);
                    int newIndex = lightX | (lightY - 1) << 5 | lightZ << 12;
                    lightNodes.add(new LightNode(newIndex, lightChunk));
                }
            }

            if (lightY + 1 > 127) {

            } else {
                if (lightChunk.getBlock(lightX, lightY + 1, lightZ) == 0 && lightChunk.getRedLight(lightX, lightY + 1, lightZ) + 2 <= lightLevel) {
                    lightChunk.setRedLight(lightX, lightY + 1, lightZ, lightLevel - 1);
                    int newIndex = lightX | (lightY + 1) << 5 | lightZ << 12;
                    lightNodes.add(new LightNode(newIndex, lightChunk));
                }
            }

            if (lightZ - 1 < 0) {
                var neighbor = world.getChunk(lightChunk.getPosition().x, lightChunk.getPosition().y - 1);
                if (neighbor.getBlock(lightX, lightY, 31) == 0 && neighbor.getRedLight(lightX, lightY, 31) + 2 <= lightLevel) {
                    neighbor.setRedLight(lightX, lightY, 31, lightLevel - 1);
                    int newIndex = lightX | lightY << 5 | 31 << 12;
                    lightNodes.add(new LightNode(newIndex, neighbor));
                }
            } else {
                if (lightChunk.getBlock(lightX, lightY, lightZ - 1) == 0 && lightChunk.getRedLight(lightX, lightY, lightZ - 1) + 2 <= lightLevel) {
                    lightChunk.setRedLight(lightX, lightY, lightZ - 1, lightLevel - 1);
                    int newIndex = lightX | lightY << 5 | (lightZ - 1) << 12;
                    lightNodes.add(new LightNode(newIndex, lightChunk));
                }
            }

            if (lightZ + 1 > 31) {
                var neighbor = world.getChunk(lightChunk.getPosition().x, lightChunk.getPosition().y + 1);
                if (neighbor.getBlock(lightX, lightY, 0) == 0 && neighbor.getRedLight(lightX, lightY, 0) + 2 <= lightLevel) {
                    neighbor.setRedLight(lightX, lightY, 0, lightLevel - 1);
                    int newIndex = lightX | lightY << 5 | 0 << 12;
                    lightNodes.add(new LightNode(newIndex, neighbor));
                }
            } else {
                if (lightChunk.getBlock(lightX, lightY, lightZ + 1) == 0 && lightChunk.getRedLight(lightX, lightY, lightZ + 1) + 2 <= lightLevel) {
                    lightChunk.setRedLight(lightX, lightY, lightZ + 1, lightLevel - 1);
                    int newIndex = lightX | lightY << 5 | (lightZ + 1) << 12;
                    lightNodes.add(new LightNode(newIndex, lightChunk));
                }
            }

            lightChunk.setDirty(true);
        }
    }

    public static void propagateGreen(World world, Queue<LightNode> lightNodes) {
        while (!lightNodes.isEmpty()) {
            var node = lightNodes.poll();

            int pos = node.position();
            var lightChunk = node.chunk();

            int lightX = pos & 0x1f;
            int lightY = (pos >> 5) & 0x7f;
            int lightZ = pos >> 12;
            int lightLevel = lightChunk.getGreenLight(lightX, lightY, lightZ);

            if (lightX - 1 < 0) {
                var neighbor = world.getChunk(lightChunk.getPosition().x - 1, lightChunk.getPosition().y);
                if (neighbor.getBlock(lightX - 1 + 32, lightY, lightZ) == 0 && neighbor.getGreenLight(lightX - 1 + 32, lightY, lightZ) + 2 <= lightLevel) {
                    neighbor.setGreenLight(lightX - 1 + 32, lightY, lightZ, lightLevel - 1);
                    int newIndex = 31 | lightY << 5 | lightZ << 12;
                    lightNodes.add(new LightNode(newIndex, neighbor));
                }
            } else {
                if (lightChunk.getBlock(lightX - 1, lightY, lightZ) == 0 && lightChunk.getGreenLight(lightX - 1, lightY, lightZ) + 2 <= lightLevel) {
                    lightChunk.setGreenLight(lightX - 1, lightY, lightZ, lightLevel - 1);
                    int newIndex = (lightX - 1) | lightY << 5 | lightZ << 12;
                    lightNodes.add(new LightNode(newIndex, lightChunk));
                }
            }

            if (lightX + 1 > 31) {
                var neighbor = world.getChunk(lightChunk.getPosition().x + 1, lightChunk.getPosition().y);
                if (neighbor.getBlock(0, lightY, lightZ) == 0 && neighbor.getGreenLight(0, lightY, lightZ) + 2 <= lightLevel) {
                    neighbor.setGreenLight(0, lightY, lightZ, lightLevel - 1);
                    int newIndex = 0 | lightY << 5 | lightZ << 12;
                    lightNodes.add(new LightNode(newIndex, neighbor));
                }
            } else {
                if (lightChunk.getBlock(lightX + 1, lightY, lightZ) == 0 && lightChunk.getGreenLight(lightX + 1, lightY, lightZ) + 2 <= lightLevel) {
                    lightChunk.setGreenLight(lightX + 1, lightY, lightZ, lightLevel - 1);
                    int newIndex = (lightX + 1) | lightY << 5 | lightZ << 12;
                    lightNodes.add(new LightNode(newIndex, lightChunk));
                }
            }

            if (lightY - 1 < 0) {

            } else {
                if (lightChunk.getBlock(lightX, lightY - 1, lightZ) == 0 && lightChunk.getGreenLight(lightX, lightY - 1, lightZ) + 2 <= lightLevel) {
                    lightChunk.setGreenLight(lightX, lightY - 1, lightZ, lightLevel - 1);
                    int newIndex = lightX | (lightY - 1) << 5 | lightZ << 12;
                    lightNodes.add(new LightNode(newIndex, lightChunk));
                }
            }

            if (lightY + 1 > 127) {

            } else {
                if (lightChunk.getBlock(lightX, lightY + 1, lightZ) == 0 && lightChunk.getGreenLight(lightX, lightY + 1, lightZ) + 2 <= lightLevel) {
                    lightChunk.setGreenLight(lightX, lightY + 1, lightZ, lightLevel - 1);
                    int newIndex = lightX | (lightY + 1) << 5 | lightZ << 12;
                    lightNodes.add(new LightNode(newIndex, lightChunk));
                }
            }

            if (lightZ - 1 < 0) {
                var neighbor = world.getChunk(lightChunk.getPosition().x, lightChunk.getPosition().y - 1);
                if (neighbor.getBlock(lightX, lightY, 31) == 0 && neighbor.getGreenLight(lightX, lightY, 31) + 2 <= lightLevel) {
                    neighbor.setGreenLight(lightX, lightY, 31, lightLevel - 1);
                    int newIndex = lightX | lightY << 5 | 31 << 12;
                    lightNodes.add(new LightNode(newIndex, neighbor));
                }
            } else {
                if (lightChunk.getBlock(lightX, lightY, lightZ - 1) == 0 && lightChunk.getGreenLight(lightX, lightY, lightZ - 1) + 2 <= lightLevel) {
                    lightChunk.setGreenLight(lightX, lightY, lightZ - 1, lightLevel - 1);
                    int newIndex = lightX | lightY << 5 | (lightZ - 1) << 12;
                    lightNodes.add(new LightNode(newIndex, lightChunk));
                }
            }

            if (lightZ + 1 > 31) {
                var neighbor = world.getChunk(lightChunk.getPosition().x, lightChunk.getPosition().y + 1);
                if (neighbor.getBlock(lightX, lightY, 0) == 0 && neighbor.getGreenLight(lightX, lightY, 0) + 2 <= lightLevel) {
                    neighbor.setGreenLight(lightX, lightY, 0, lightLevel - 1);
                    int newIndex = lightX | lightY << 5 | 0 << 12;
                    lightNodes.add(new LightNode(newIndex, neighbor));
                }
            } else {
                if (lightChunk.getBlock(lightX, lightY, lightZ + 1) == 0 && lightChunk.getGreenLight(lightX, lightY, lightZ + 1) + 2 <= lightLevel) {
                    lightChunk.setGreenLight(lightX, lightY, lightZ + 1, lightLevel - 1);
                    int newIndex = lightX | lightY << 5 | (lightZ + 1) << 12;
                    lightNodes.add(new LightNode(newIndex, lightChunk));
                }
            }

            lightChunk.setDirty(true);
        }
    }

    public static void propagateBlue(World world, Queue<LightNode> lightNodes) {
        while (!lightNodes.isEmpty()) {
            var node = lightNodes.poll();

            int pos = node.position();
            var lightChunk = node.chunk();

            int lightX = pos & 0x1f;
            int lightY = (pos >> 5) & 0x7f;
            int lightZ = pos >> 12;
            int lightLevel = lightChunk.getBlueLight(lightX, lightY, lightZ);

            if (lightX - 1 < 0) {
                var neighbor = world.getChunk(lightChunk.getPosition().x - 1, lightChunk.getPosition().y);
                if (neighbor.getBlock(lightX - 1 + 32, lightY, lightZ) == 0 && neighbor.getBlueLight(lightX - 1 + 32, lightY, lightZ) + 2 <= lightLevel) {
                    neighbor.setBlueLight(lightX - 1 + 32, lightY, lightZ, lightLevel - 1);
                    int newIndex = 31 | lightY << 5 | lightZ << 12;
                    lightNodes.add(new LightNode(newIndex, neighbor));
                }
            } else {
                if (lightChunk.getBlock(lightX - 1, lightY, lightZ) == 0 && lightChunk.getBlueLight(lightX - 1, lightY, lightZ) + 2 <= lightLevel) {
                    lightChunk.setBlueLight(lightX - 1, lightY, lightZ, lightLevel - 1);
                    int newIndex = (lightX - 1) | lightY << 5 | lightZ << 12;
                    lightNodes.add(new LightNode(newIndex, lightChunk));
                }
            }

            if (lightX + 1 > 31) {
                var neighbor = world.getChunk(lightChunk.getPosition().x + 1, lightChunk.getPosition().y);
                if (neighbor.getBlock(0, lightY, lightZ) == 0 && neighbor.getBlueLight(0, lightY, lightZ) + 2 <= lightLevel) {
                    neighbor.setBlueLight(0, lightY, lightZ, lightLevel - 1);
                    int newIndex = 0 | lightY << 5 | lightZ << 12;
                    lightNodes.add(new LightNode(newIndex, neighbor));
                }
            } else {
                if (lightChunk.getBlock(lightX + 1, lightY, lightZ) == 0 && lightChunk.getBlueLight(lightX + 1, lightY, lightZ) + 2 <= lightLevel) {
                    lightChunk.setBlueLight(lightX + 1, lightY, lightZ, lightLevel - 1);
                    int newIndex = (lightX + 1) | lightY << 5 | lightZ << 12;
                    lightNodes.add(new LightNode(newIndex, lightChunk));
                }
            }

            if (lightY - 1 < 0) {

            } else {
                if (lightChunk.getBlock(lightX, lightY - 1, lightZ) == 0 && lightChunk.getBlueLight(lightX, lightY - 1, lightZ) + 2 <= lightLevel) {
                    lightChunk.setBlueLight(lightX, lightY - 1, lightZ, lightLevel - 1);
                    int newIndex = lightX | (lightY - 1) << 5 | lightZ << 12;
                    lightNodes.add(new LightNode(newIndex, lightChunk));
                }
            }

            if (lightY + 1 > 127) {

            } else {
                if (lightChunk.getBlock(lightX, lightY + 1, lightZ) == 0 && lightChunk.getBlueLight(lightX, lightY + 1, lightZ) + 2 <= lightLevel) {
                    lightChunk.setBlueLight(lightX, lightY + 1, lightZ, lightLevel - 1);
                    int newIndex = lightX | (lightY + 1) << 5 | lightZ << 12;
                    lightNodes.add(new LightNode(newIndex, lightChunk));
                }
            }

            if (lightZ - 1 < 0) {
                var neighbor = world.getChunk(lightChunk.getPosition().x, lightChunk.getPosition().y - 1);
                if (neighbor.getBlock(lightX, lightY, 31) == 0 && neighbor.getBlueLight(lightX, lightY, 31) + 2 <= lightLevel) {
                    neighbor.setBlueLight(lightX, lightY, 31, lightLevel - 1);
                    int newIndex = lightX | lightY << 5 | 31 << 12;
                    lightNodes.add(new LightNode(newIndex, neighbor));
                }
            } else {
                if (lightChunk.getBlock(lightX, lightY, lightZ - 1) == 0 && lightChunk.getBlueLight(lightX, lightY, lightZ - 1) + 2 <= lightLevel) {
                    lightChunk.setBlueLight(lightX, lightY, lightZ - 1, lightLevel - 1);
                    int newIndex = lightX | lightY << 5 | (lightZ - 1) << 12;
                    lightNodes.add(new LightNode(newIndex, lightChunk));
                }
            }

            if (lightZ + 1 > 31) {
                var neighbor = world.getChunk(lightChunk.getPosition().x, lightChunk.getPosition().y + 1);
                if (neighbor.getBlock(lightX, lightY, 0) == 0 && neighbor.getBlueLight(lightX, lightY, 0) + 2 <= lightLevel) {
                    neighbor.setBlueLight(lightX, lightY, 0, lightLevel - 1);
                    int newIndex = lightX | lightY << 5 | 0 << 12;
                    lightNodes.add(new LightNode(newIndex, neighbor));
                }
            } else {
                if (lightChunk.getBlock(lightX, lightY, lightZ + 1) == 0 && lightChunk.getBlueLight(lightX, lightY, lightZ + 1) + 2 <= lightLevel) {
                    lightChunk.setBlueLight(lightX, lightY, lightZ + 1, lightLevel - 1);
                    int newIndex = lightX | lightY << 5 | (lightZ + 1) << 12;
                    lightNodes.add(new LightNode(newIndex, lightChunk));
                }
            }

            lightChunk.setDirty(true);
        }
    }

    public static void propagateSun(World world, Queue<LightNode> lightNodes) {
        while (!lightNodes.isEmpty()) {
            var node = lightNodes.poll();

            int pos = node.position();
            var lightChunk = node.chunk();

            int lightX = pos & 0x1f;
            int lightY = (pos >> 5) & 0x7f;
            int lightZ = pos >> 12;
            int lightLevel = lightChunk.getSunLight(lightX, lightY, lightZ);

            if (lightX - 1 < 0) {
                var neighbor = world.getChunk(lightChunk.getPosition().x - 1, lightChunk.getPosition().y);
                if (neighbor != null && neighbor.getBlock(lightX - 1 + 32, lightY, lightZ) == 0 && neighbor.getSunLight(lightX - 1 + 32, lightY, lightZ) + 2 <= lightLevel) {
                    neighbor.setSunLight(lightX - 1 + 32, lightY, lightZ, lightLevel - 1);
                    int newIndex = 31 | lightY << 5 | lightZ << 12;
                    lightNodes.add(new LightNode(newIndex, neighbor));
                }
            } else {
                if (lightChunk.getBlock(lightX - 1, lightY, lightZ) == 0 && lightChunk.getSunLight(lightX - 1, lightY, lightZ) + 2 <= lightLevel) {
                    lightChunk.setSunLight(lightX - 1, lightY, lightZ, lightLevel - 1);
                    int newIndex = (lightX - 1) | lightY << 5 | lightZ << 12;
                    lightNodes.add(new LightNode(newIndex, lightChunk));
                }
            }

            if (lightX + 1 > 31) {
                var neighbor = world.getChunk(lightChunk.getPosition().x + 1, lightChunk.getPosition().y);
                if (neighbor != null && neighbor.getBlock(0, lightY, lightZ) == 0 && neighbor.getSunLight(0, lightY, lightZ) + 2 <= lightLevel) {
                    neighbor.setSunLight(0, lightY, lightZ, lightLevel - 1);
                    int newIndex = 0 | lightY << 5 | lightZ << 12;
                    lightNodes.add(new LightNode(newIndex, neighbor));
                }
            } else {
                if (lightChunk.getBlock(lightX + 1, lightY, lightZ) == 0 && lightChunk.getSunLight(lightX + 1, lightY, lightZ) + 2 <= lightLevel) {
                    lightChunk.setSunLight(lightX + 1, lightY, lightZ, lightLevel - 1);
                    int newIndex = (lightX + 1) | lightY << 5 | lightZ << 12;
                    lightNodes.add(new LightNode(newIndex, lightChunk));
                }
            }

            if (lightY - 1 < 0) {

            } else {
                if (lightChunk.getBlock(lightX, lightY - 1, lightZ) == 0 && lightChunk.getSunLight(lightX, lightY - 1, lightZ) + 2 <= lightLevel) {
                    lightChunk.setSunLight(lightX, lightY - 1, lightZ, lightLevel - 1);
                    int newIndex = lightX | (lightY - 1) << 5 | lightZ << 12;
                    lightNodes.add(new LightNode(newIndex, lightChunk));
                }
            }

            if (lightY + 1 > 127) {

            } else {
                if (lightChunk.getBlock(lightX, lightY + 1, lightZ) == 0 && lightChunk.getSunLight(lightX, lightY + 1, lightZ) + 2 <= lightLevel) {
                    lightChunk.setSunLight(lightX, lightY + 1, lightZ, lightLevel - 1);
                    int newIndex = lightX | (lightY + 1) << 5 | lightZ << 12;
                    lightNodes.add(new LightNode(newIndex, lightChunk));
                }
            }

            if (lightZ - 1 < 0) {
                var neighbor = world.getChunk(lightChunk.getPosition().x, lightChunk.getPosition().y - 1);
                if (neighbor != null && neighbor.getBlock(lightX, lightY, 31) == 0 && neighbor.getSunLight(lightX, lightY, 31) + 2 <= lightLevel) {
                    neighbor.setSunLight(lightX, lightY, 31, lightLevel - 1);
                    int newIndex = lightX | lightY << 5 | 31 << 12;
                    lightNodes.add(new LightNode(newIndex, neighbor));
                }
            } else {
                if (lightChunk.getBlock(lightX, lightY, lightZ - 1) == 0 && lightChunk.getSunLight(lightX, lightY, lightZ - 1) + 2 <= lightLevel) {
                    lightChunk.setSunLight(lightX, lightY, lightZ - 1, lightLevel - 1);
                    int newIndex = lightX | lightY << 5 | (lightZ - 1) << 12;
                    lightNodes.add(new LightNode(newIndex, lightChunk));
                }
            }

            if (lightZ + 1 > 31) {
                var neighbor = world.getChunk(lightChunk.getPosition().x, lightChunk.getPosition().y + 1);
                if (neighbor != null && neighbor.getBlock(lightX, lightY, 0) == 0 && neighbor.getSunLight(lightX, lightY, 0) + 2 <= lightLevel) {
                    neighbor.setSunLight(lightX, lightY, 0, lightLevel - 1);
                    int newIndex = lightX | lightY << 5 | 0 << 12;
                    lightNodes.add(new LightNode(newIndex, neighbor));
                }
            } else {
                if (lightChunk.getBlock(lightX, lightY, lightZ + 1) == 0 && lightChunk.getSunLight(lightX, lightY, lightZ + 1) + 2 <= lightLevel) {
                    lightChunk.setSunLight(lightX, lightY, lightZ + 1, lightLevel - 1);
                    int newIndex = lightX | lightY << 5 | (lightZ + 1) << 12;
                    lightNodes.add(new LightNode(newIndex, lightChunk));
                }
            }
        }
    }

}
