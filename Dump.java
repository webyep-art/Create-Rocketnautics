import dev.simulated_team.simulated.content.blocks.rope.strand.client.RopeStrandRenderer;
import java.lang.reflect.Method;

public class Dump {
    public static void main(String[] args) {
        for (Method m : RopeStrandRenderer.class.getDeclaredMethods()) {
            System.out.println(m.getName() + " " + m.getParameterCount());
            for (Class<?> p : m.getParameterTypes()) {
                System.out.println("  - " + p.getName());
            }
        }
    }
}
