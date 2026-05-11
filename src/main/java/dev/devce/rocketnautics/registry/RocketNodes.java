package dev.devce.rocketnautics.registry;

import dev.devce.rocketnautics.RocketNautics;
import dev.devce.rocketnautics.content.blocks.SputnikBlockEntity;
import dev.devce.rocketnautics.api.peripherals.IPeripheral;
import dev.devce.websnodelib.api.NodeRegistry;
import dev.devce.websnodelib.api.WNode;
import net.minecraft.resources.ResourceLocation;

public class RocketNodes {
    public static void register() {
        // --- Sensors ---
        
        NodeRegistry.register(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "altitude"), (x, y) -> {
            WNode node = new WNode(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "altitude"), "Altitude", x, y);
            node.addOutput("m", 0xFF00AAFF);
            node.setEvaluator(n -> {
                if (n.getParentGraph().getContext() instanceof SputnikBlockEntity sputnik) {
                    n.getOutputs().get(0).setValue(sputnik.getAltitude());
                }
            });
            return node;
        });

        NodeRegistry.register(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "velocity"), (x, y) -> {
            WNode node = new WNode(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "velocity"), "Velocity", x, y);
            node.addOutput("m/s", 0xFF00AAFF);
            node.setEvaluator(n -> {
                if (n.getParentGraph().getContext() instanceof SputnikBlockEntity sputnik) {
                    n.getOutputs().get(0).setValue(sputnik.getVelocity());
                }
            });
            return node;
        });

        NodeRegistry.register(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "attitude"), (x, y) -> {
            WNode node = new WNode(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "attitude"), "Attitude", x, y);
            node.addOutput("Pitch", 0xFFFFAA00);
            node.addOutput("Yaw", 0xFFFFAA00);
            node.addOutput("Roll", 0xFFFFAA00);
            node.setEvaluator(n -> {
                if (n.getParentGraph().getContext() instanceof SputnikBlockEntity sputnik) {
                    n.getOutputs().get(0).setValue(sputnik.getPitch());
                    n.getOutputs().get(1).setValue(sputnik.getYaw());
                    n.getOutputs().get(2).setValue(sputnik.getRoll());
                }
            });
            return node;
        });

        NodeRegistry.register(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "attitude_display"), (x, y) -> {
            WNode node = new WNode(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "attitude_display"), "Attitude Display", x, y);
            
            // Add inputs for debugging/wiring
            node.addInput("Pitch", 0xFFFFAA00);
            node.addInput("Yaw", 0xFFFFAA00);
            node.addInput("Roll", 0xFFFFAA00);
            
            // Full-width viewport
            dev.devce.websnodelib.api.elements.WViewport3D viewport = new dev.devce.websnodelib.api.elements.WViewport3D(120, 120);
            viewport.setZoom(0.8f);
            
            // Tighten the Y spacing to 0.5f since GUI item rendering scales them down by 50%
            // Rotate the thruster 180 degrees on X so the nozzle points down
            viewport.addModel(new net.minecraft.world.item.ItemStack(RocketBlocks.ROCKET_THRUSTER.get()), new org.joml.Vector3f(0, -0.75f, 0), new org.joml.Vector3f(180, 0, 0), 1.0f);
            viewport.addModel(new net.minecraft.world.item.ItemStack(net.minecraft.world.level.block.Blocks.IRON_BLOCK), new org.joml.Vector3f(0, -0.25f, 0), new org.joml.Vector3f(0, 0, 0), 1.0f);
            viewport.addModel(new net.minecraft.world.item.ItemStack(net.minecraft.world.level.block.Blocks.IRON_BLOCK), new org.joml.Vector3f(0, 0.25f, 0), new org.joml.Vector3f(0, 0, 0), 1.0f);
            viewport.addModel(new net.minecraft.world.item.ItemStack(net.minecraft.world.level.block.Blocks.LIGHTNING_ROD), new org.joml.Vector3f(0, 0.75f, 0), new org.joml.Vector3f(0, 0, 0), 1.0f);
            
            node.addElement(viewport);
            
            node.setEvaluator(n -> {
                // Read from pins instead of directly from the context
                float pitch = (float) n.getInputs().get(0).getValue();
                float yaw = (float) n.getInputs().get(1).getValue();
                float roll = (float) n.getInputs().get(2).getValue();
                viewport.setGlobalRotation(pitch, yaw, roll);
            });
            return node;
        });

        // --- Actuators ---

        NodeRegistry.register(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "peripheral_list"), (x, y) -> {
            WNode node = new WNode(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "peripheral_list"), "Peripheral List", x, y);
            
            // Custom element to draw the list
            dev.devce.websnodelib.api.WElement listElement = new dev.devce.websnodelib.api.WElement() {
                private java.util.List<String> lines = new java.util.ArrayList<>();
                
                {
                    this.width = 160;
                    this.height = 16;
                }
                
                public void render(net.minecraft.client.gui.GuiGraphics graphics, int ex, int ey, int mouseX, int mouseY, float partialTick) {
                    if (lines.isEmpty()) {
                        graphics.drawString(net.minecraft.client.Minecraft.getInstance().font, "No engines found.", ex + 4, ey + 4, 0xFF888888, false);
                    } else {
                        for (int i = 0; i < lines.size(); i++) {
                            graphics.drawString(net.minecraft.client.Minecraft.getInstance().font, lines.get(i), ex + 4, ey + 4 + i * 12, 0xFF00FF88, false);
                        }
                    }
                }

                public void update(java.util.List<String> newLines) {
                    this.lines = newLines;
                    this.height = lines.isEmpty() ? 16 : lines.size() * 12 + 8;
                }
            };
            
            node.addElement(listElement);
            
            node.setEvaluator(n -> {
                if (n.getParentGraph().getContext() instanceof SputnikBlockEntity sputnik) {
                    java.util.List<String> current = new java.util.ArrayList<>();
                    java.util.List<dev.devce.rocketnautics.api.peripherals.IPeripheral> periphs = sputnik.getPeripherals();
                    for (int i = 0; i < periphs.size(); i++) {
                        dev.devce.rocketnautics.api.peripherals.IPeripheral p = periphs.get(i);
                        if (p != null) {
                            current.add("- " + p.getPeripheralType().toUpperCase() + " [" + i + "]");
                        }
                    }
                    // Update the element
                    try {
                        for (java.lang.reflect.Method m : n.getElements().get(0).getClass().getDeclaredMethods()) {
                            if (m.getName().equals("update")) {
                                m.setAccessible(true);
                                m.invoke(n.getElements().get(0), current);
                            }
                        }
                        n.updateLayout();
                    } catch (Exception e) {}
                }
            });
            return node;
        });

        NodeRegistry.register(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "thruster_control"), (x, y) -> {
            WNode node = new WNode(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "thruster_control"), "Rocket Thruster Control", x, y);
            node.addInput("ID", 0xFF888888);
            node.addInput("Power", 0xFFFF5555);
            node.setEvaluator(n -> {
                if (n.getParentGraph().getContext() instanceof SputnikBlockEntity sputnik) {
                    int id = (int) n.getInputs().get(0).getValue();
                    double power = n.getInputs().get(1).getValue();
                    var periphs = sputnik.getPeripherals();
                    if (id >= 0 && id < periphs.size()) {
                        var p = periphs.get(id);
                        if (p != null && p.getPeripheralType().equals("thruster")) {
                            p.writeValue("thrust", power);
                        }
                    }
                }
            });
            return node;
        });

        NodeRegistry.register(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "vector_control"), (x, y) -> {
            WNode node = new WNode(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "vector_control"), "Vector Control", x, y);
            node.addInput("ID", 0xFF888888);
            node.addInput("Pitch", 0xFFFFAA00);
            node.addInput("Yaw", 0xFFFFAA00);
            node.addInput("Power", 0xFFFF5555);
            node.setEvaluator(n -> {
                if (n.getParentGraph().getContext() instanceof SputnikBlockEntity sputnik) {
                    int id = (int) n.getInputs().get(0).getValue();
                    double pitch = n.getInputs().get(1).getValue();
                    double yaw = n.getInputs().get(2).getValue();
                    double power = n.getInputs().get(3).getValue();
                    var periphs = sputnik.getPeripherals();
                    if (id >= 0 && id < periphs.size()) {
                        var p = periphs.get(id);
                        if (p != null && p.getPeripheralType().equals("vector_engine")) {
                            p.writeValues("gimbal", pitch, yaw);
                            p.writeValue("thrust", power);
                        }
                    }
                }
            });
            return node;
        });

        NodeRegistry.register(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "rcs_control"), (x, y) -> {
            WNode node = new WNode(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "rcs_control"), "RCS Control", x, y);
            node.addInput("ID", 0xFF888888);
            node.addInput("Power", 0xFFFF5555);
            node.setEvaluator(n -> {
                if (n.getParentGraph().getContext() instanceof SputnikBlockEntity sputnik) {
                    int id = (int) n.getInputs().get(0).getValue();
                    double power = n.getInputs().get(1).getValue();
                    var periphs = sputnik.getPeripherals();
                    if (id >= 0 && id < periphs.size()) {
                        var p = periphs.get(id);
                        if (p != null && p.getPeripheralType().equals("rcs")) {
                            p.writeValue("thrust", power);
                        }
                    }
                }
            });
            return node;
        });

        NodeRegistry.register(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "booster_control"), (x, y) -> {
            WNode node = new WNode(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "booster_control"), "Booster Control", x, y);
            node.addInput("ID", 0xFF888888);
            node.addInput("Power", 0xFFFF5555);
            node.setEvaluator(n -> {
                if (n.getParentGraph().getContext() instanceof SputnikBlockEntity sputnik) {
                    int id = (int) n.getInputs().get(0).getValue();
                    double power = n.getInputs().get(1).getValue();
                    var periphs = sputnik.getPeripherals();
                    if (id >= 0 && id < periphs.size()) {
                        var p = periphs.get(id);
                        if (p != null && p.getPeripheralType().equals("booster")) {
                            p.writeValue("thrust", power);
                        }
                    }
                }
            });
            return node;
        });

        NodeRegistry.register(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "redstone_out"), (x, y) -> {
            WNode node = new WNode(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "redstone_out"), "Redstone Out", x, y);
            node.addInput("Level", 0xFFFF0000);
            node.setEvaluator(n -> {
                if (n.getParentGraph().getContext() instanceof SputnikBlockEntity sputnik) {
                    int level = (int) net.minecraft.util.Mth.clamp(n.getInputs().get(0).getValue(), 0, 15);
                    sputnik.setOutput("all", level);
                }
            });
            return node;
        });

        // --- Create Integration: Linked Redstone ---

        // Transmitter
        NodeRegistry.register(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "redstone_transmitter"), (x, y) -> {
            WNode node = new WNode(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "redstone_transmitter"), "Linked Transmitter", x, y);
            node.addInput("Signal", 0xFFFF5555);
            
            dev.devce.websnodelib.api.elements.WItemPicker freq1 = new dev.devce.websnodelib.api.elements.WItemPicker();
            dev.devce.websnodelib.api.elements.WItemPicker freq2 = new dev.devce.websnodelib.api.elements.WItemPicker();
            freq1.setBorderColor(0xFFFF3333); // Red
            freq2.setBorderColor(0xFF3333FF); // Blue
            
            node.addElement(new dev.devce.websnodelib.api.elements.WLabel("Freq 1 (Red):"));
            node.addElement(freq1);
            node.addElement(new dev.devce.websnodelib.api.elements.WLabel("Freq 2 (Blue):"));
            node.addElement(freq2);
            
            node.setEvaluator(n -> {
                if (n.getParentGraph().getContext() instanceof SputnikBlockEntity sputnik) {
                    if (freq1.getStack().isEmpty() || freq2.getStack().isEmpty()) return;
                    
                    double signal = n.getInputs().get(0).getValue();
                    dev.devce.rocketnautics.content.blocks.LinkedSignalHandler.setSignal(
                        sputnik.getLevel(), 
                        freq1.getStack(), 
                        freq2.getStack(), 
                        sputnik.getBlockPos(), 
                        signal
                    );
                }
            });
            return node;
        });

        // Receiver
        NodeRegistry.register(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "redstone_receiver"), (x, y) -> {
            WNode node = new WNode(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "redstone_receiver"), "Linked Receiver", x, y);
            node.addOutput("Signal", 0xFFFFAA00);
            
            dev.devce.websnodelib.api.elements.WItemPicker freq1 = new dev.devce.websnodelib.api.elements.WItemPicker();
            dev.devce.websnodelib.api.elements.WItemPicker freq2 = new dev.devce.websnodelib.api.elements.WItemPicker();
            freq1.setBorderColor(0xFFFF3333); // Red
            freq2.setBorderColor(0xFF3333FF); // Blue
            
            node.addElement(new dev.devce.websnodelib.api.elements.WLabel("Freq 1 (Red):"));
            node.addElement(freq1);
            node.addElement(new dev.devce.websnodelib.api.elements.WLabel("Freq 2 (Blue):"));
            node.addElement(freq2);
            
            node.setEvaluator(n -> {
                if (n.getParentGraph().getContext() instanceof SputnikBlockEntity sputnik) {
                    if (freq1.getStack().isEmpty() || freq2.getStack().isEmpty()) {
                        n.getOutputs().get(0).setValue(0);
                        return;
                    }
                    
                    double signal = dev.devce.rocketnautics.content.blocks.LinkedSignalHandler.getSignal(
                        sputnik.getLevel(), 
                        freq1.getStack(), 
                        freq2.getStack(), 
                        sputnik.getBlockPos()
                    );
                    n.getOutputs().get(0).setValue(signal);
                }
            });
            return node;
        });
        // --- MATH & LOGIC NODES (Turing Complete Set) ---

        // Constant Value
        NodeRegistry.register(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "constant"), (x, y) -> {
            WNode node = new WNode(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "constant"), "Constant", x, y);
            node.addOutput("Value", 0xFFFFFFFF);
            dev.devce.websnodelib.api.elements.WTextField field = new dev.devce.websnodelib.api.elements.WTextField(40);
            field.setValue("0");
            node.addElement(field);
            node.setEvaluator(n -> {
                try {
                    n.getOutputs().get(0).setValue(Double.parseDouble(field.getValue()));
                } catch (Exception e) {
                    n.getOutputs().get(0).setValue(0);
                }
            });
            return node;
        });

        // Math Operations (Add, Sub, Mul, Div)
        registerMathNode("add", "Add", (a, b) -> a + b);
        registerMathNode("sub", "Subtract", (a, b) -> a - b);
        registerMathNode("mul", "Multiply", (a, b) -> a * b);
        registerMathNode("div", "Divide", (a, b) -> b != 0 ? a / b : 0);

        // Comparison (Greater, Less, Equal)
        registerMathNode("greater", "Greater Than", (a, b) -> a > b ? 1.0 : 0.0);
        registerMathNode("less", "Less Than", (a, b) -> a < b ? 1.0 : 0.0);
        registerMathNode("equal", "Equal", (a, b) -> Math.abs(a - b) < 0.0001 ? 1.0 : 0.0);

        // Logic (AND, OR, XOR)
        registerMathNode("and", "Logic AND", (a, b) -> (a > 0 && b > 0) ? 1.0 : 0.0);
        registerMathNode("or", "Logic OR", (a, b) -> (a > 0 || b > 0) ? 1.0 : 0.0);
        registerMathNode("xor", "Logic XOR", (a, b) -> (a > 0 ^ b > 0) ? 1.0 : 0.0);

        // NOT
        NodeRegistry.register(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "not"), (x, y) -> {
            WNode node = new WNode(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "not"), "Logic NOT", x, y);
            node.addInput("In", 0xFFFFFFFF);
            node.addOutput("Out", 0xFFFFFFFF);
            node.setEvaluator(n -> n.getOutputs().get(0).setValue(n.getInputs().get(0).getValue() > 0 ? 0.0 : 1.0));
            return node;
        });

        // Select (If/Else)
        NodeRegistry.register(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "select"), (x, y) -> {
            WNode node = new WNode(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "select"), "Select", x, y);
            node.addInput("Condition", 0xFFFFFF00); // Yellow for condition
            node.addInput("If True", 0xFFFFFFFF);
            node.addInput("If False", 0xFFFFFFFF);
            node.addOutput("Result", 0xFFFFFFFF);
            node.setEvaluator(n -> {
                double cond = n.getInputs().get(0).getValue();
                double valTrue = n.getInputs().get(1).getValue();
                double valFalse = n.getInputs().get(2).getValue();
                n.getOutputs().get(0).setValue(cond > 0 ? valTrue : valFalse);
            });
            return node;
        });

        // Display Node
        NodeRegistry.register(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "display"), (x, y) -> {
            WNode node = new WNode(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, "display"), "Display", x, y);
            node.addInput("In", 0xFFFFFFFF);
            dev.devce.websnodelib.api.elements.WLabel label = new dev.devce.websnodelib.api.elements.WLabel("0.00", 0xFF00FF00); // Green text
            node.addElement(label);
            node.setEvaluator(n -> {
                double val = n.getInputs().get(0).getValue();
                label.setText(String.format("%.2f", val));
            });
            return node;
        });
    }

    private static void registerMathNode(String id, String name, java.util.function.BiFunction<Double, Double, Double> op) {
        NodeRegistry.register(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, id), (x, y) -> {
            WNode node = new WNode(ResourceLocation.fromNamespaceAndPath(RocketNautics.MODID, id), name, x, y);
            node.addInput("A", 0xFFFFFFFF);
            node.addInput("B", 0xFFFFFFFF);
            node.addOutput("Out", 0xFFFFFFFF);
            node.setEvaluator(n -> {
                double a = n.getInputs().get(0).getValue();
                double b = n.getInputs().get(1).getValue();
                n.getOutputs().get(0).setValue(op.apply(a, b));
            });
            return node;
        });
    }
}
