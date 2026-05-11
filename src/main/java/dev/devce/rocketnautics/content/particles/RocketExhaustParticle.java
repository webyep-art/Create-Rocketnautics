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
        this.lifetime = 6 + this.random.nextInt(6);
        this.baseScale = 0.4F + this.random.nextFloat() * 0.6F; 
        this.quadSize = this.baseScale;
        
        if (sprites != null) {
            this.setSpriteFromAge(sprites);
        } else {
            this.remove(); 
        }
        
        this.hasPhysics = true; 
        this.friction = 0.90F;  
        this.gravity = 0.01F;   
        
        
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
            
            
            float animScale = this.shrinking ? (1.0f - ageFactor * 0.9f) : (1.0f + ageFactor * 1.5f);
            this.quadSize = this.baseScale * animScale * (0.8f + this.random.nextFloat() * 0.4f);
            
            this.rCol = r;
            this.gCol = g;
            this.bCol = b;
            
            
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
        return 15728880; 
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
            
            
            double speed = Math.sqrt(xSpeed * xSpeed + ySpeed * ySpeed + zSpeed * zSpeed);
            float thrustFactor = (float) ((speed - 2.5) / 1.5);
            if (thrustFactor < 0) thrustFactor = 0;
            if (thrustFactor > 1) thrustFactor = 1;

            String name = net.minecraft.core.registries.BuiltInRegistries.PARTICLE_TYPE.getKey(type).getPath();
            
            if (name.contains("blue_flame")) {
                particle.setColor(0.2f, 0.5f, 1.0f); 
                particle.setCoolingColor(0.4f, 0.1f, 0.8f); 
            } else if (name.contains("plasma")) {
                // Explosion Core - White Hot
                particle.setColor(1.0F, 1.0F, 0.8F); 
                particle.setCoolingColor(1.0F, 0.6F, 0.2F); 
            } else if (name.contains("plume")) {
                // Main Fireball - Orange Red
                particle.setColor(1.0F, 0.6F, 0.1F); 
                particle.setCoolingColor(0.8F, 0.1F, 0.0F); 
            } else {
                particle.setColor(1.0f, 1.0f, 1.0f); 
                particle.setCoolingColor(0.5f, 0.5f, 0.5f); 
            }
            
            particle.gravity = 0.0F; 
            
            if (name.contains("plasma")) {
                particle.setLifetime(6 + level.random.nextInt(6));
                particle.scale(0.7f);
                particle.setColor(1.0F, 1.0F, 1.0F); 
                particle.setCoolingColor(1.0F, 0.8F, 0.4F);
                particle.setShrinking(true);
            } else if (name.contains("plume")) {
                particle.setLifetime(10 + level.random.nextInt(8));
                particle.scale(0.85f);
                particle.setColor(1.0F, 0.7F, 0.2F);
                particle.setCoolingColor(0.25F, 0.25F, 0.25F); 
                particle.setShrinking(true);
            } else if (name.contains("blue_flame")) {
                particle.setLifetime(4 + level.random.nextInt(4));
                particle.scale(0.2f);
                particle.setAlpha(0.9f);
                particle.setShrinking(true);
            }
            
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
            
            double speed = Math.sqrt(xSpeed * xSpeed + ySpeed * ySpeed + zSpeed * zSpeed);
            if (speed > 0.1) {
                // Ground impact smoke - short lived (1 second)
                particle.setLifetime(15 + level.random.nextInt(10)); 
            } else {
                // Contrail smoke - long lived (15 seconds)
                particle.setLifetime(280 + level.random.nextInt(40)); 
            }
            particle.scale(1.2f + level.random.nextFloat() * 0.8f);
            particle.setShrinking(false); 
            particle.friction = 0.98F; 
            particle.setMaxAlpha(0.5F);
            particle.gravity = 0.0F; // Stop it from falling
            
            particle.setColor(1.0F, 1.0F, 1.0F); // Pure white
            particle.setCoolingColor(0.9F, 0.9F, 0.9F);
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
            particle.setLifetime(6 + level.random.nextInt(6)); 
            particle.scale(0.4f + level.random.nextFloat() * 0.4f); 
            particle.friction = 0.98F; 
            particle.setShrinking(true);
            particle.setMaxAlpha(1.0F); 
            particle.gravity = 0.0F; 
            particle.hasPhysics = false; 
            
            
            particle.setColor(1.0F, 1.0F, 1.0F);
            particle.setCoolingColor(1.0F, 1.0F, 1.0F);
            return particle;
        }
    }
}
