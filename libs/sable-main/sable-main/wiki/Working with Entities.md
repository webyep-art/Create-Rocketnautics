## "Entity Kicking"

Sable, by default, will "kick" all entities that are spawned inside the plot of a sub-level to global space.
This teleports the entity to its global position, applies velocity from the sub-level, and transforms the velocity and rotation of the entity out of the sub-level.

For some entities, such as Paintings or Armor Stands, this behavior is undesired, and the intended outcome is for the entity to stay inside the sub-level.
Sable therefore has tags to customize how entities interact with entity kicking:

- `#sable:retain_in_sub_level` - Never kick this entity from sub-levels. (ex. Armor Stands, Paintings)
- `#sable:destroy_when_leaving_plot` - Destroy this entity when it is inside a sub-level plot, but exits the bounds containing the sub-level blocks.
- `#sable:destroy_with_sub_level` - Destroy this entity when the sub-level plot containing it is destroyed, instead of kicking it to the global world. (ex. Super Glue from Create)


### Examples

To specify that an entity should stay inside of sub-level plots, and should never be kicked:
```js
// /data/sable/tags/entity_type/retain_in_sub_level.json
{
  "replace": false,
  "values": [
    "examplemod:example_entity"
  ]
}
```

## Tracking
Entities can be *outside* of the plot of a sub-level, but still move with the sub-level (ex. A player standing on a sub-level, or a cow in a pen on a sub-level). When entities are standing on sub-levels, Sable marks them as "tracking" the sub-level. 

Entities that are tracking a sub-level:
- Are networked relative to the sub-level
- Are interpolated relative to the sub-level
- Will move with the sub-level as it rotates and translates

Players that are tracking a sub-level will additionally log-out and log-in with a position relative to the sub-level through the tracking points system.

Sable has utilities to check the tracking sub-level of an entity:
```java
Entity entity = ...;
SubLevel subLevel = EntitySubLevelUtil.getTrackingSubLevel(this.entity);
```
