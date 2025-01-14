package org.pepsoft.worldpainter.importing;

import com.google.common.primitives.Bytes;
import org.pepsoft.util.MathUtils;
import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.layers.Annotations;
import org.pepsoft.worldpainter.layers.Layer;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.util.Random;

import static java.awt.RenderingHints.KEY_DITHERING;
import static java.awt.RenderingHints.VALUE_DITHER_ENABLE;
import static java.awt.image.BufferedImage.TYPE_BYTE_INDEXED;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

abstract class Mapping {
    Mapping(String aspect, String description) {
        this.aspect = aspect;
        this.description = description;
    }

    String getAspect() {
        return aspect;
    }

    String getDescription() {
        return description;
    }

    void setTile(Tile tile) {
        this.tile = tile;
    }

    void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    void setMaskLowValue(int maskLowValue) {
        this.maskLowValue = maskLowValue;
    }

    void setMaskHighValue(int maskHighValue) {
        this.maskHighValue = maskHighValue;
    }

    void setMaskMaxValue(int maskMaxValue) {
        this.maskMaxValue = maskMaxValue;
    }

    abstract void apply(int x, int y, int maskValue);

    Mapping ditheredActualRange() {
        return new Mapping(aspect, description + " (dithered from actual mask range)") {
            @Override
            void apply(int x, int y, int maskValue) {
                if ((maskValue >= maskHighValue) || ((maskValue > maskLowValue) && (maskValue > (random.nextInt(maskHighValue - maskLowValue) + maskLowValue)))) {
                    Mapping.this.apply(x, y, maskValue);
                }
            }

            @Override
            void setTile(Tile tile) {
                super.setTile(tile);
                Mapping.this.setTile(tile);
            }

            @Override
            void setThreshold(int threshold) {
                super.setThreshold(threshold);
                Mapping.this.setThreshold(threshold);
            }

            @Override
            void setMaskLowValue(int maskLowValue) {
                super.setMaskLowValue(maskLowValue);
                Mapping.this.setMaskLowValue(maskLowValue);
            }

            @Override
            void setMaskHighValue(int maskHighValue) {
                super.setMaskHighValue(maskHighValue);
                Mapping.this.setMaskHighValue(maskHighValue);
            }

            @Override
            void setMaskMaxValue(int maskMaxValue) {
                super.setMaskMaxValue(maskMaxValue);
                Mapping.this.setMaskMaxValue(maskMaxValue);
            }

            private final Random random = new Random(0L);
        };
    }

    Mapping ditheredFullRange() {
        return new Mapping(aspect, description + " (dithered from full mask range)") {
            @Override
            void apply(int x, int y, int maskValue) {
                if ((maskValue > 0) && (maskValue > random.nextInt(maskMaxValue))) {
                    Mapping.this.apply(x, y, maskValue);
                }
            }

            @Override
            void setTile(Tile tile) {
                super.setTile(tile);
                Mapping.this.setTile(tile);
            }

            @Override
            void setThreshold(int threshold) {
                super.setThreshold(threshold);
                Mapping.this.setThreshold(threshold);
            }

            @Override
            void setMaskLowValue(int maskLowValue) {
                super.setMaskLowValue(maskLowValue);
                Mapping.this.setMaskLowValue(maskLowValue);
            }

            @Override
            void setMaskHighValue(int maskHighValue) {
                super.setMaskHighValue(maskHighValue);
                Mapping.this.setMaskHighValue(maskHighValue);
            }

            @Override
            void setMaskMaxValue(int maskMaxValue) {
                super.setMaskMaxValue(maskMaxValue);
                Mapping.this.setMaskMaxValue(maskMaxValue);
            }

            private final Random random = new Random(0L);
        };
    }

    static abstract class ThresholdMapping extends Mapping {
        ThresholdMapping(String aspect, String description) {
            super(aspect, description);
        }
    }

    ThresholdMapping threshold() {
        return new ThresholdMapping(aspect, description + " where mask is at or above threshold") {
            @Override
            void apply(int x, int y, int maskValue) {
                if (maskValue >= threshold) {
                    Mapping.this.apply(x, y, maskValue);
                }
            }

            @Override
            void setTile(Tile tile) {
                super.setTile(tile);
                Mapping.this.setTile(tile);
            }

            @Override
            void setThreshold(int threshold) {
                super.setThreshold(threshold);
                Mapping.this.setThreshold(threshold);
            }

            @Override
            void setMaskLowValue(int maskLowValue) {
                super.setMaskLowValue(maskLowValue);
                Mapping.this.setMaskLowValue(maskLowValue);
            }

            @Override
            void setMaskHighValue(int maskHighValue) {
                super.setMaskHighValue(maskHighValue);
                Mapping.this.setMaskHighValue(maskHighValue);
            }

            @Override
            void setMaskMaxValue(int maskMaxValue) {
                super.setMaskMaxValue(maskMaxValue);
                Mapping.this.setMaskMaxValue(maskMaxValue);
            }
        };
    }

    static Mapping setTerrainValue(Terrain terrain) {
        return new Mapping("terrain " + terrain, "Set terrain type to " + terrain) {
            @Override
            void apply(int x, int y, int maskValue) {
                if (maskValue != 0) {
                    tile.setTerrain(x, y, terrain);
                }
            }
        };
    }

    static Mapping setLayerValue(Layer layer, int targetValue) {
        if (layer.dataSize.maxValue == 1) {
            return new Mapping("layer " + layer + " (value " + targetValue + ")", "Set layer " + layer + " to selected value") {
                @Override
                void apply(int x, int y, int maskValue) {
                    if ((targetValue != 0) && (maskValue != 0)) {
                        tile.setBitLayerValue(layer, x, y, true);
                    }
                }
            };
        } else if (layer.discrete) {
            return new Mapping("layer " + layer + " (value " + targetValue + ")", "Set layer " + layer + " to selected value") {
                @Override
                void apply(int x, int y, int maskValue) {
                    if (maskValue != 0) {
                        tile.setLayerValue(layer, x, y, targetValue);
                    }
                }
            };
        } else {
            return new Mapping("layer " + layer + " (value " + targetValue + ")", "Set layer " + layer + " to selected value") {
                @Override
                void apply(int x, int y, int maskValue) {
                    tile.setLayerValue(layer, x, y, Math.max(targetValue, tile.getLayerValue(layer, x, y)));
                }
            };
        }
    }

    static Mapping mapToTerrain() {
        return new Mapping("terrain", "Set terrain type index to mask value") {
            @Override
            void apply(int x, int y, int maskValue) {
                tile.setTerrain(x, y, Terrain.VALUES[maskValue]);
            }
        };
    }

    static Mapping mapToLayer(Layer layer) {
        if (layer.dataSize.maxValue == 1) {
            return new Mapping("layer " + layer, "Set layer " + layer + " to mask value") {
                @Override
                void apply(int x, int y, int maskValue) {
                    if (maskValue != 0) {
                        tile.setBitLayerValue(layer, x, y, true);
                    }
                }
            };
        } else {
            return new Mapping("layer " + layer, "Set layer " + layer + " to mask value") {
                @Override
                void apply(int x, int y, int maskValue) {
                    if (maskValue != 0) {
                        tile.setLayerValue(layer, x, y, Math.max(maskValue, tile.getLayerValue(layer, x, y)));
                    }
                }
            };
        }
    }

    abstract static class RangedMapping extends Mapping {
        RangedMapping(String aspect, String description) {
            super(aspect, description);
        }
    }

    static Mapping mapActualRangeToLayer(Layer layer) {
        if (layer.dataSize.maxValue == 1) {
            throw new IllegalArgumentException("layer.dataSize " + layer.dataSize);
        }
        if (layer.discrete) {
            return new Mapping("layer " + layer, "Map layer " + layer + " to actual mask range") {
                @Override
                void apply(int x, int y, int maskValue) {
                    if (maskValue != 0) {
                        tile.setLayerValue(layer, x, y, (maskValue - maskLowValue) * layer.dataSize.maxValue / (maskHighValue - maskLowValue));
                    }
                }
            };
        } else {
            return new RangedMapping("layer " + layer, "Map layer " + layer + " to actual mask range") {
                @Override
                void apply(int x, int y, int maskValue) {
                    tile.setLayerValue(layer, x, y, Math.max((maskValue - maskLowValue) * layer.dataSize.maxValue / (maskHighValue - maskLowValue), tile.getLayerValue(layer, x, y)));
                }
            };
        }
    }

    static Mapping mapFullRangeToLayer(Layer layer) {
        if (layer.dataSize.maxValue == 1) {
            throw new IllegalArgumentException("layer.dataSize " + layer.dataSize);
        }
        if (layer.discrete) {
            return new Mapping("layer " + layer, "Map layer " + layer + " to full mask range") {
                @Override
                void apply(int x, int y, int maskValue) {
                    if (maskValue != 0) {
                        tile.setLayerValue(layer, x, y, maskValue * layer.dataSize.maxValue / maskMaxValue);
                    }
                }
            };
        } else {
            return new RangedMapping("layer " + layer, "Map layer " + layer + " to full mask range") {
                @Override
                void apply(int x, int y, int maskValue) {
                    tile.setLayerValue(layer, x, y, Math.max(maskValue * layer.dataSize.maxValue / maskMaxValue, tile.getLayerValue(layer, x, y)));
                }
            };
        }
    }

    abstract static class ColourToAnnotationsMapping extends Mapping {
        ColourToAnnotationsMapping(String aspect, String description) {
            super(aspect, description);
        }

        boolean dithered;
    }

    static ColourToAnnotationsMapping colourToAnnotations() {
        return new ColourToAnnotationsMapping("annotations", "Map layer Annotations to mask colours") {
            @Override
            void apply(int x, int y, int maskValue) {
                if (((maskValue >> 24) & 0xff) > 0x7f) {
                    tile.setLayerValue(Annotations.INSTANCE, x, y, COLOUR_ANNOTATION_MAPPING[((maskValue >> 12) & 0xf00) | ((maskValue >> 8) & 0xf0) | ((maskValue >> 4) & 0xf)]);
                }
            }

            @Override
            ColourToAnnotationsMapping ditheredActualRange() {
                return new ColourToAnnotationsMapping("annotations", "Map layer Annotations to mask colours (dithered)") {
                    {
                        dithered = true;
                    }

                    @Override
                    void apply(int x, int y, int maskValue) {
                        if (((maskValue >> 24) & 0xff) > 0x7f) {
                            tile.setLayerValue(Annotations.INSTANCE, x, y, COLOUR_ANNOTATION_MAPPING[((maskValue >> 12) & 0xf00) | ((maskValue >> 8) & 0xf0) | ((maskValue >> 4) & 0xf)]);
                        }
                    }
                };
            }
        };
    }

    static BufferedImage ditherMask(BufferedImage maskImage) {
        final IndexColorModel colorModel = new IndexColorModel(4, 15,
                Bytes.toArray(stream(ANNOTATIONS_PALETTE).map(row -> row[0]).collect(toList())),
                Bytes.toArray(stream(ANNOTATIONS_PALETTE).map(row -> row[1]).collect(toList())),
                Bytes.toArray(stream(ANNOTATIONS_PALETTE).map(row -> row[2]).collect(toList())));
        final BufferedImage ditheredImage = new BufferedImage(maskImage.getWidth(), maskImage.getHeight(), TYPE_BYTE_INDEXED, colorModel);
        final Graphics2D g2 = ditheredImage.createGraphics();
        try {
            g2.setRenderingHint(KEY_DITHERING, VALUE_DITHER_ENABLE);
            g2.drawImage(maskImage, 0, 0, null);
        } finally {
            g2.dispose();
        }
        return ditheredImage;
    }

    private static int findNearestAnnotationValue(int colour) {
        final int red = (colour & 0xff0000) >> 16;
        final int green = (colour & 0xff00) >> 8;
        final int blue = colour & 0xff;
        float minDistance = Float.MAX_VALUE;
        int minDistanceIndex = -1;
        for (int i = 0; i < ANNOTATIONS_PALETTE.length; i++) {
            final float distance = MathUtils.getDistance(red - ANNOTATIONS_PALETTE[i][0], green - ANNOTATIONS_PALETTE[i][1], blue - ANNOTATIONS_PALETTE[i][2]);
            if (distance < minDistance) {
                minDistance = distance;
                minDistanceIndex = i;
            }
        }
        return minDistanceIndex + 1;
    }

    private final String aspect, description;
    protected Tile tile;
    protected int threshold, maskLowValue, maskHighValue, maskMaxValue;

    private static final int[][] ANNOTATIONS_PALETTE = {
            {0xdd, 0xdd, 0xdd},
            {0xdb, 0x7d, 0x3e},
            {0xb3, 0x50, 0xbc},
            {0x6a, 0x8a, 0xc9},
            {0xb1, 0xa6, 0x27},
            {0x41, 0xae, 0x38},
            {0xd0, 0x84, 0x99},
            {0x9a, 0xa1, 0xa1},
            {0x2e, 0x6e, 0x89},
            {0x7e, 0x3d, 0xb5},
            {0x2e, 0x38, 0x8d},
            {0x4f, 0x32, 0x1f},
            {0x35, 0x46, 0x1b},
            {0x96, 0x34, 0x30},
            {0x19, 0x16, 0x16}
    };

    /**
     * A table which maps colours to annotation layer values. The colours have
     * their last four bits stripped to keep the table small.
     */
    private static final int[] COLOUR_ANNOTATION_MAPPING = new int[4096];

    static {
        for (int i = 0; i < 4096; i++) {
            COLOUR_ANNOTATION_MAPPING[i] = findNearestAnnotationValue(((i & 0xf00) << 12) | ((i & 0xf0) << 8) | ((i & 0xf) << 4));
        }
    }
}