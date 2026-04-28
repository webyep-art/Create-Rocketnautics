Sable stores physics-related properties per block-state.
These properties are defined through definition JSONs in datapacks.

### Available Properties

The default available properties consist of:
- `sable:mass` - mass of the block in `kpg`. Default `1.0`
- `sable:inertia` - inertia multiplier of the block along each axis in `kpg*m^2`. Is multiplied by the mass of the block before usage. Default `[1/6, 1/6, 1/6]`
- `sable:volume` - the volume of the block in `m^3`. Used for buoyancy calculations. Default `1.0`
- `sable:restitution` - the bounciness of the block from 0-1. Default `0.0`
- `sable:friction` - the friction multiplier of the block. Default `1.0`
- `sable:fragile` - if the block should break upon impact. Default `false`
- `sable:floating_material` - the floating block material to assign. Default `null`
- `sable:floating_scale` - the multiplier for the floating block material. Default `1.0`

### JSON Structure

Block physics property definition JSONs can be put in any datapack under the `physics_block_properties` folder.

```js
// /data/examplemod/physics_block_properties/example_block.json
{
    // The selector can either be a tag, or block ID.
    // If a tag is used, all blocks in the tag will be effected.
    // Ex. `#examplemod:example_blocks` or `examplemod:example_block`
    "selector": "examplemod:example_block"

    // Priority is default 1000.
    // Definitions are applied in order of ascending priority
    "priority": 1001,

    "properties": {
        // Any properties can be defined here
        "sable:mass": 2.0
    },

    "overrides": {
        // Override keys are block-state conditions
        "lit=true": {
            // Any properties can be defined here
            // All block-states meeting the condition will be affected
            "sable:mass": 3.0
        }
    }

}
```


### Examples

A block that bounces:

```js
// /data/examplemod/physics_block_properties/bouncy_block.json
{
  "selector": "examplemod:bouncy_block",

  "properties": {
    "sable:restitution": 0.5
  }
}
```

A piston that doesn't weigh as much when extended:

```js
// /data/examplemod/physics_block_properties/piston.json
{
  "selector": "examplemod:piston",

  "properties": {
    "sable:mass": 1.0
  },

  "overrides": {
    "extended=true": {
      "sable:mass": 0.5
    }
  }
}
```

### Tags

Sable contains many block tags in its own built-in datapack for commonly used physics block properties. 
It is suggested to put your block into the pre-defined tags, if you do not need custom property definitions:

- `#sable:super_light` mass = 0.25
- `#sable:light` mass = 0.5
- `#sable:heavy` mass = 2.0
- `#sable:super_heavy` mass = 4.0

- `#sable:half_volume` volume = 0.5
- `#sable:quarter_volume` volume = 0.25

- `#sable:slippery` friction = 0.0
- `#sable:bouncy` restitution = 0.5