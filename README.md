**Warning: This project is still in the experimental phase.**

The concept of dynamic deobfuscation mappings is a rather unexplored one, and the softmap project
is dedicated to test the usability of deobfuscation mappings which aren't static.

As such, design paradigms may shift, resulting in serious API breakages or even syntax changes.
It is also be possible that the concept might not bear any fruits (as in I realize that this
project is utter nonsense) and as such silently be
buried in your local software graveyard.

# Softmap

A dynamic deobfuscation mappings format. In classical reverse engineering,
deobfuscating some software feels a lot like assembling a puzzle:
You start by one piece - say a string constant - from which you can deduce
the meaning of adjacent pieces. Softmap adopts this style of thinking
and applies it to deobfuscation mappings. Basically, you write out everything
you do know about a method (owner, name, desc, instructions, etc.) and the
Softmap algorithm goes over it and tries to find the best match and inferr
(map) the information marked to be inferred.

# Examples

**Note:** This tool is experimental and as such the syntax may change in future.
Furthermore, quite a lot of opcodes are not or improperly implemented.
Proceed with caution, there be dragons.


```
softmap v1

method snoddasmannen/galimulator/Star.drawStarBorders?()V {
	*
	getstatic snoddasmannen/galimulator/Settings$EnumSettings.DRAW_STAR_BORDERS Lsnoddasmannen/galimulator/Settings$EnumSettings;
	invokevirtual snoddasmannen/galimulator/Settings$EnumSettings.getValue()Ljava/lang/Object;
	*
	aload this
	getfield snoddasmannen/galimulator/Star.starRenderingRegion Lcom/badlogic/gdx/graphics/g2d/PolygonSprite;
	*
	aload this
	getfield snoddasmannen/galimulator/Star.regionVertices? [F
	*
	aload this
	getstatic snoddasmannen/galimulator/Star$PolygonType.SOLID Lsnoddasmannen/galimulator/Star$PolygonType;
	invokevirtual snoddasmannen/galimulator/Star.createCellRegion?(Lsnoddasmannen/galimulator/Star$PolygonType;)V
	*
}
```

Comments are performed java-style, though they should be separated using whitespace
as right now the parser is rather fragile when it comes to comments.
