# KMLib UI Map Transform

Part of [KMLib UI](../../README.md).

What the cursor is over on the sector map.
An overlay draws world coordinates and the mouse arrives as a window pixel,
and nothing in the game's API inverts the map's pan, centring and zoom,
so an overlay that wants to hit-test what it drew has to undo that transform itself.
This is where that inversion lives,
along with the one thing it cannot work without:
the matrix the map was drawn under,
which not every renderer will hand back.

## Index

- [Whether the matrix can be read is the renderer's business](#whether-the-matrix-can-be-read-is-the-renderers-business)
- [Three bindings](#three-bindings)
- [When Fast Rendering refuses the read](#when-fast-rendering-refuses-the-read)
- [One reader per consumer](#one-reader-per-consumer)
- [From a pixel to a world point](#from-a-pixel-to-a-world-point)
- [What is not here](#what-is-not-here)

## Whether the matrix can be read is the renderer's business

[`ModelviewMatrixReader`](ModelviewMatrixReader.java) is the port.
Every binding that reads asks GL the same thing,
`glGetFloat(GL_MODELVIEW_MATRIX)`,
but not every renderer answers it.
The stock renderer always does.
Fast Rendering keeps the matrix in a Java object rather than in GL,
and answers the read from a copy of it only from `v0.9.1rc1`;
earlier releases refuse it mid-render.
Depending on the port keeps the transform maths free of that,
and free of a GL static nothing can stand in for.

The renderer facts themselves are not here.
They live in [`kmlib.opengl.FastRendering`](../../../../opengl/FastRendering.java),
and their citations in [`docs/dev/rendering-environment.md`](../../../../../../../../docs/dev/rendering-environment.md).

## Three bindings

| Binding | Reads | For |
| --- | --- | --- |
| [`GlModelviewMatrixReader`](GlModelviewMatrixReader.java) | `GL_MODELVIEW_MATRIX`, read back | the stock renderer |
| [`FastRenderingModelviewMatrixReader`](FastRenderingModelviewMatrixReader.java) | the same read, guarded | Fast Rendering |
| [`UnavailableModelviewMatrixReader`](UnavailableModelviewMatrixReader.java) | nothing, on every read | a caller with no matrix to read |

[`ModelviewMatrixReaders`](ModelviewMatrixReaders.java) picks between the first two.
None of them names a Fast Rendering type,
so all three load and run on any install.

## When Fast Rendering refuses the read

A release before `v0.9.1rc1` refuses the read where it is asked for,
inside a render pass:
`UnsupportedOperationException` from `v0.8.9`,
where the bridge declares the method and refuses the call,
and `NoSuchMethodError` before it,
where the method was not declared at all.
Let out of the pass,
either costs the game over a hover highlight.

So the Fast Rendering binding takes the read under one catch for both shapes.
The first refusal latches the reading off for the session,
so no later frame asks again,
and every read from then on is no reading rather than a guessed one -
which [`CampaignMapTransform`](CampaignMapTransform.java) already reads as "park rather than guess".
One failure is recorded however many frames the map stays open.
It names the release the read needs,
and the one the installed jar reports,
so the player is told which release to update to.
Where the report then goes is [`starsector/compatibility/`](../../../compatibility/README.md).

## One reader per consumer

The sentence naming what a refused read costs comes from the mod taking the reading, not from here.
This package knows the renderer, both versions and what was thrown,
and nothing at all about what was drawn over the reading.

That is why a guarded reader is built per consumer rather than shared.
It records its own failure,
and one that could not say who it serves could record it against nobody -
or, worse, against whichever mod happened to ask first.
Two mods reading the map under Fast Rendering therefore hold a reader each.

## From a pixel to a world point

[`CampaignMapTransform`](CampaignMapTransform.java) is a value snapshot rather than a live reader,
because its inputs exist only for an instant -
the map widget sets its matrices up around its render pass and tears them down after,
so the modelview describes the map only from inside `renderOnMap`.
`captureFromMapPass` freezes it there and the value stays usable for the rest of the frame.
Two coordinate steps stack and both are undone:
`gluUnProject` inverts the pan and centring baked into the matrices,
and the uniform zoom, which reaches the vertices per-coordinate at draw time instead,
is divided out afterwards.
The inversion itself is pure arithmetic over the snapshot's own arrays,
so the part that is actually easy to get wrong is verifiable with no GL context.

[`MapCursor`](MapCursor.java) folds the three steps between a cursor and a world point into one call -
is the cursor on the window, snapshot the pass's transform, invert it -
because each has its own way of coming back with no usable answer
and all of them mean the same thing to a caller.
It reads the cursor rather than consuming input events,
so the map goes on hovering entities and drawing their tooltips exactly as it did.
[`MapCursorRead`](MapCursorRead.java) is what comes back:
the pixel, the transform it was mapped through and the point that came out, as one value,
because a wrong hover is a disagreement between them
and none of them is diagnosable without the others.

## What is not here

- What a world point *means*.
  Which of an overlay's shapes covers it is the overlay's question,
  and nothing here knows anything about one.
- The renderer facts.
  [`kmlib.opengl`](../../../../opengl/) holds whether the bridge is in force and how its matrices are laid out,
  and the probe that names which mirrored member no longer holds.
- The report.
  [`starsector/compatibility/`](../../../compatibility/README.md) holds the record a failure is filed in
  and the notice that shows it.
