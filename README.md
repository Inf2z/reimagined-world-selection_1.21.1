## Reimagined World Selection
A brand new look for the World Selection Screen!

### Features:
- Highly Configurable
- Integration with KubeJS
- Built-in Dynamic Updating Icons
- Custom Lines With Your Custom Info
  
### Information:

_How to start working with custom info?_

You can easily do it with KubeJS! Start your client-side script with:

`const RWS = Java.loadClass('com.inf2z.reimagined_world_selection.api.WorldSelectionAPI')`

And then you have many ways to manipulate those 3 custom lines!
First of all, every line has a variable. Those variables have 3 modes (configurable in-game).

### Modes:
- Boolean (If the value is <= 0, it displays as False. If the value is >= 1, it displays as True.)
- ON/OFF (Similar to Boolean, but displays OFF and ON instead.)
- Value (Just displays the raw value or text.)
  
With KubeJS, you have many ways to manipulate these values.

### Methods:
- RWS.updateVar(lineNumber, value) — Updates the specified line (1, 2, or 3) with a new string value.
- RWS.saveCurrent(var1, var2, var3) — Mass updates all 3 lines at once.
- RWS.addValue(lineNumber, amount) — Adds a numeric amount to the current variable. Automatically removes trailing zeros (e.g., 5.0 becomes 5). If the current variable is text, it counts it as 0.
- RWS.resetVar(lineNumber) — Resets the specified line's value back to "0".
- RWS.getString(lineNumber) — Gets the current value of the line as text.
- RWS.getNumber(lineNumber) — Gets the current value of the line as a number (returns 0 if the value is text).
- RWS.freeze(lineNumber) — Freezes the variable. It will ignore any further updates or additions until unfrozen.
- RWS.unfreeze(lineNumber) — Unfreezes the variable, allowing it to be changed again.

### Config Examples:
- Custom Line (1-3) Label: Killed / Golden Leggings is / Cool Guy
- Custom Line (1-3) Format: %var% Zombies / %var% / %var%
- Custom Line (1-3) Mode: VALUE / ON_OFF / BOOLEAN
  
##### Note: Variable data is saved securely inside each specific world's folder (reimagined_vars.json), meaning your stats are always tied to the correct save!
##### P.S: This mod was by 99.9% AI-coded :P (at least not made in MCreator)
##### P.P.S: I was heavily inspired by [Reimagined World Menu](https://modrinth.com/mod/reimagined-world-menu), so also check out this mod!! :D
