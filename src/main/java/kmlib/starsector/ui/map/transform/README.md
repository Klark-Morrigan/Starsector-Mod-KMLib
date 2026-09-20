# KMLib UI Map Transform

Part of [KMLib UI](../../README.md).

What the cursor is over on the sector map.
An overlay draws world coordinates and the mouse arrives as a window pixel,
and nothing in the game's API inverts the map's pan, centring and zoom,
so an overlay that wants to hit-test what it drew has to undo that transform itself.
This is where that inversion lives,
along with the one thing it cannot work without:
the matrix the map was drawn under,
which is somewhere different depending on which renderer is installed.

## Index

- [Where the matrix lives is the renderer's business](#where-the-matrix-lives-is-the-renderers-business)
- [Three bindings](#three-bindings)
- [Reading a deferred renderer's matrix](#reading-a-deferred-renderers-matrix)
- [When the bridge stops holding](#when-the-bridge-stops-holding)
- [One reader per consumer](#one-reader-per-consumer)
- [From a pixel to a world point](#from-a-pixel-to-a-world-point)
- [What is not here](#what-is-not-here)

## Where the matrix lives is the renderer's business

[`ModelviewMatrixReader`](ModelviewMatrixReader.java) is the port,
and "read it back from GL" is one binding of that role rather than the definition of it.
The stock renderer keeps the modelview in GL,
so GL is authoritative under it.
Fast Rendering tracks the matrix in a Java object and multiplies each vertex by it before submitting,
leaving GL's own modelview as identity -
so a GL read-back under that renderer reports a matrix describing nothing.
Depending on the port keeps the transform maths free of that choice,
and free of a third-party static nothing can stand in for.

The renderer facts themselves are not here.
They live in [`kmlib.opengl.FastRendering`](../../../../opengl/FastRendering.java),
and their citations in [`docs/dev/rendering-environment.md`](../../../../../../../../docs/dev/rendering-environment.md).

## Three bindings

| Binding | Reads | For |
| --- | --- | --- |
| [`GlModelviewMatrixReader`](GlModelviewMatrixReader.java) | `GL_MODELVIEW_MATRIX`, read back | the stock renderer |
| [`FastRenderingModelviewMatrixReader`](FastRenderingModelviewMatrixReader.java) | the renderer's own `TransformManager`, a frame late | Fast Rendering |
| [`UnavailableModelviewMatrixReader`](UnavailableModelviewMatrixReader.java) | nothing, on every read | a renderer whose matrix cannot be reached |

[`ModelviewMatrixReaders`](ModelviewMatrixReaders.java) picks between them,
which is also what keeps the bridge-bound reader unreachable on an install that cannot load it:
the reference is resolved inside a method body,
so a branch never taken never loads the class.

The third is not a fallback onto the first.
Under a renderer that tracks the modelview on the CPU,
GL's own copy is identity,
so falling back to it would trade a crash for a matrix that describes nothing -
a wrong answer rather than no answer.
The degraded state is no reading, never a guessed one,
and [`CampaignMapTransform`](CampaignMapTransform.java) already reads an absent matrix as "park rather than guess".

## Reading a deferred renderer's matrix

Fast Rendering's matrix cannot be read where the caller stands,
and cannot be read synchronously either.
A `glTranslatef` on the calling thread only appends a command to a frame buffer,
and the `TransformManager` it mutates lives on a render thread replaying that buffer a step behind,
so an inline read samples an unrelated in-flight transform, torn field by field.
Forcing the read to complete would read the right matrix and stall the pipeline every frame,
which that renderer's stall detector turns into a fatal error.

So the read is deferred.
Each call enqueues a fire-and-forget command that copies the matrix on the render thread
at the enqueuing pass's own position in the stream,
where it is the map widget's transform and stable,
and the call returns the copy a prior frame's command left behind.
The result is a frame or two old,
which is invisible for a still map and trails by a frame or two of pan velocity while panning.

[`FastRenderingModelviewCopy`](FastRenderingModelviewCopy.java) is that copy,
held apart from the binding because the two answer to different threads and to different failures.

## When the bridge stops holding

None of what the bridge binding reads is published API,
and it has been relocated between releases without notice,
so it can stop holding in two places that fail in two different ways.

**At link time**, when the reader's class initialises:
a `LinkageError` naming a class or a member that moved,
raised inside whichever render pass reached it first.
`ModelviewMatrixReaders` takes the binding under one catch for all three shapes of that -
a class that is gone, a member that is gone, a signature that changed -
degrades onto the unavailable reader and records the failure.

**At call time**, inside the enqueued command,
which is the harder one and the reason the copy is its own class.
A command that throws where the renderer runs it is not caught where it was enqueued:
the renderer captures it and re-throws it wrapped on the game thread at the next frame swap,
outside every KM stack frame,
so a guard around the enqueue never sees it and the game dies over a hover highlight.
The only place it can be contained is inside the command body,
so the copy is total by construction rather than by being short enough to look safe -
and the matrix read is taken *inside* that guard rather than before it,
the bridge member answering it being as able to stop holding as the copy is.

Either way the answer is the same:
the reading latches unavailable for the session,
no stale matrix is reported in its place,
and one failure is recorded however many frames the map stays open.
Both sides compose that report through [`FastRenderingBridgeFailures`](FastRenderingBridgeFailures.java),
so a player is told one thing about one renderer whichever side noticed.
Where the report then goes is [`starsector/compatibility/`](../../../compatibility/README.md).

## One reader per consumer

The sentence naming what a failed binding costs comes from the mod taking the reading, not from here.
This package knows the renderer, both versions, the member that moved and what was thrown,
and nothing at all about what was drawn over the reading.

That is why a reader is built per consumer rather than shared.
A reader records its own call-time failures,
and one that could not say who it serves could record them against nobody -
or, worse, against whichever mod happened to ask first.
Two mods reading the map therefore hold a reader each
and, under the bridge, enqueue a copy each:
one small command per frame per reader,
which is the price of a report that names the right mod.

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
