# Port checks (new Minecraft release, B10 alwaysLatest)

Static checks for porting to a new Minecraft version. They read the decompiled source that `tools/mcsrc.py` unpacks into `build/mcsrc/<version>/` and need no game run. Run them after the bump, once the mods compile.

| Script | Usage | Finds |
|---|---|---|
| `compile_errors.py` | `python tools/portcheck/compile_errors.py <gradle output file>` | one line per distinct javac error, with its symbol/required/found lines |
| `mixin_targets.py` | `python tools/portcheck/mixin_targets.py <repo> build/mcsrc/<new> [<fabric-api checkout>]` | mixin target classes, `method =`, `@Accessor`/`@Invoker` and `@At` targets that no longer exist by name |
| `mixin_signatures.py` | `python tools/portcheck/mixin_signatures.py <repo> build/mcsrc/<old> build/mcsrc/<new>` | mixin-targeted methods whose parameter list changed |
| `mixin_shadows.py` | `python tools/portcheck/mixin_shadows.py <repo> build/mcsrc/<new>` | `@Shadow` members missing from the target class chain |
| `removed_json.py` | `python tools/portcheck/removed_json.py build/mcsrc/<old> build/mcsrc/<new> <repo>` | vanilla JSON keys and `minecraft:` ids gone in the new version, and our files that still use them |

All are heuristics (text matching): run the name check on the old version too, as a control, and ignore what it also reports (inherited members, other mods' classes). String `targets =` mixins are not covered by the signature check; compare those methods by hand.
