package dev.devce.rocketnautics.content.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.NotNull;

public class RocketExhaustParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    protected RocketExhaustParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.sprites = sprites;
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.setAlpha(0.4F + this.random.nextFloat() * 0.3F);
        this.lifetime = 20 + this.random.nextInt(10);
        this.baseScale = 0.4F + this.random.nextFloat() * 0.6F; // 2x bigger than previous tiny state
        this.quadSize = this.baseScale;
        
        if (sprites != null) {
            this.setSpriteFromAge(sprites);
        } else {
            this.remove(); // Failsafe: if no sprites, remove immediately
        }
        
        this.hasPhysics = true; // Enable collisions with blocks!
        this.friction = 0.98F;  // Slight air resistance
        this.gravity = 0.01F;   // Slight gravity to make them fall eventually (optional)
        
        // Coloring for flame default
        this.rCol = 1.0F;
        this.gCol = 0.6F + this.random.nextFloat() * 0.4F;
        this.bCol = 0.2F;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            if (this.sprites != null) {
                this.setSpriteFromAge(this.sprites);
            }
            
            // Three-stage color interpolation:
            // 0.0 -> 0.2: White to Target (Blue/Orange)
            // 0.2 -> 1.0: Target to Cooling Color (Yellow/Orange)
            float ageFactor = (float)this.age / (float)this.lifetime;
            
            float r, g, b;
            float startR = 1.0f, startG = 1.0f, startB = 1.0f;
            if (ageFactor < 0.2f) {
                float f = ageFactor / 0.2f;
                r = startR + (this.targetR - startR) * f;
                g = startG + (this.targetG - startG) * f;
                b = startB + (this.targetB - startB) * f;
            } else {
                float f = (ageFactor - 0.2f) / 0.8f;
                r = this.targetR + (this.coolingR - this.targetR) * f;
                g = this.targetG + (this.coolingG - this.targetG) * f;
                b = this.targetB + (this.coolingB - this.targetB) * f;
            }
            
            // Expanding or shrinking plume effect
            float animScale = this.shrinking ? (1.0f - ageFactor * 0.9f) : (1.0f + ageFactor * 1.5f);
            this.quadSize = this.baseScale * animScale * (0.8f + this.random.nextFloat() * 0.4f);
            
            this.rCol = r;
            this.gCol = g;
            this.bCol = b;
            
            // Fade out alpha over time
            this.alpha = (1.0f - ageFactor) * this.maxAlpha;

            this.move(this.xd, this.yd, this.zd);
            
            this.xd *= this.friction;
            this.yd *= this.friction;
            this.zd *= this.friction;
            this.yd -= this.gravity;
        }
    }

    private float targetR = 1.0f;
    private float targetG = 1.0f;
    private float targetB = 1.0f;

    private float coolingR = 1.0f;
    private float coolingG = 0.5f;
    private float coolingB = 0.1f;
    
    private boolean shrinking = false;
    private float maxAlpha = 0.7f;
    private float baseScale = 1.0f;

    @Override
    public void setColor(float r, float g, float b) {
        super.setColor(r, g, b);
        this.targetR = r;
        this.targetG = g;
        this.targetB = b;
        // Start as pure white
        this.rCol = 1.0f;
        this.gCol = 1.0f;
        this.bCol = 1.0f;
    }

    public void setCoolingColor(float r, float g, float b) {
        this.coolingR = r;
        this.coolingG = g;
        this.coolingB = b;
    }

    public void setShrinking(boolean shrinking) {
        this.shrinking = shrinking;
    }

    public void setMaxAlpha(float alpha) {
        this.maxAlpha = alpha;
    }

    @Override
    public Particle scale(float scale) {
        this.baseScale *= scale;
        return super.scale(scale);
    }

    @Override
    public int getLightColor(float partialTick) {
        return 15728880; // Full brightness (Lightmap.FULL_BRIGHT)
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class FlameProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public FlameProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            RocketExhaustParticle particle = new RocketExhaustParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
            
            // Determine thrust factor for color mode
            double speed = Math.sqrt(xSpeed * xSpeed + ySpeed * ySpeed + zSpeed * zSpeed);
            float thrustFactor = (float) ((speed - 2.5) / 1.5);
            if (thrustFactor < 0) thrustFactor = 0;
            if (thrustFactor > 1) thrustFactor = 1;

            String name = net.minecraft.core.registries.BuiltInRegistries.PARTICLE_TYPE.getKey(type).getPath();
            
            if (name.contains("blue_flame") && thrustFactor > 0.4f) {
                // HIGH POWER PLASMA MODE (Blue -> Purple/Dark)
                particle.setColor(0.2f, 0.5f, 1.0f); // Vibrant Blue
                particle.setCoolingColor(0.4f, 0.1f, 0.8f); // Cooling into Purple
            } else if (name.contains("plasma")) {
                particle.setColor(0.3F, 0.8F, 1.0F); // Cyan plasma
                particle.setCoolingColor(0.1F, 0.4F, 1.0F); // Deep blue
            } else {
                // STANDARD ROCKET MODE (Orange -> Red/Dark)
                particle.setColor(1.0f, 0.6f, 0.1f); // Hot Orange
                particle.setCoolingColor(0.8f, 0.1f, 0.0f); // Cooling into Red
            }
            
            particle.gravity = 0.0F; // Fire doesn't fall
            return particle;
        }
    }

    public static class SmokeProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public SmokeProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            RocketExhaustParticle particle = new RocketExhaustParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
            particle.setLifetime(20 + level.random.nextInt(15)); // Shorter but visible
            particle.scale(1.2f + level.random.nextFloat() * 0.8f);
            
            // Pure white smoke for rockets
            particle.setColor(1.0F, 1.0F, 1.0F);
            return particle;
        }
    }

    public static class RCSGasProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public RCSGasProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            RocketExhaustParticle particle = new RocketExhaustParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
            particle.setLifetime(15 + level.random.nextInt(10)); // Lives longer to fly further
            particle.scale(0.4f + level.random.nextFloat() * 0.4f); // 2x smaller than before
            particle.friction = 0.98F; // Standard air resistance
            particle.setShrinking(true);
            particle.setMaxAlpha(1.0F); // Pure solid white at start
            particle.gravity = 0.0F; // Gas doesn't fall down
            particle.hasPhysics = false; // Prevent gas from getting stuck inside the thruster block
            
            // Pure white gas that stays white
            particle.setColor(1.0F, 1.0F, 1.0F);
            particle.setCoolingColor(1.0F, 1.0F, 1.0F);
            return particle;
        }
    }
}
