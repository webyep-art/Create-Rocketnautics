Sable's sub-levels each contain their own lighting sections, and lighting data.

We need Flywheel shaders to be aware of this, so we change and override the lighting storage, LUT, and shaders to respect an additional "scene ID".

I'm not happy with the large amounts of duplicated shader code in these overrides, but it's the route we are going with for now.

Reference https://github.com/Engine-Room/Flywheel/tree/1.21.1/dev for the original shaders and lighting code that these overrides are based on.