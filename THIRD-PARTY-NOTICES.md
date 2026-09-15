# Third-party notices

Code in this repository derived from third-party projects, and who holds copyright in it.

KMLib is licensed under the GNU Lesser General Public License version 3
([LICENSE](LICENSE), with the GPL it incorporates at [LICENSE.GPL](LICENSE.GPL)),
which is the licence the code below already carried.

## Index

- [Starficz - ReflectionUtils](#starficz---reflectionutils)

## Starficz - ReflectionUtils

The reflection facility in [`kmlib/starsector/ui/coreui/`](src/main/java/kmlib/starsector/ui/coreui/)
is derived from `org.magiclib.ReflectionUtils`,
distributed in [MagicLib](https://github.com/MagicLibStarsector/MagicLib) and copyright Starficz under LGPL-3.0-only.
Its own header credits Lukas04 for his ReflectionUtils,
and Lyravega, Float and Andylizi for the original idea.

MagicLib as a whole is MIT-licensed, but that file is not:
it carries its own copyright and licence header, which governs it.
No MIT-licensed MagicLib code is used here.

The derived files, modified in 2026 - rewritten in Java, split by concern,
restated as query records rather than default-argument overloads,
and extended with a hierarchy walk for methods a superclass declares without publishing:

- [`ReflectionBypass`](src/main/java/kmlib/starsector/ui/coreui/ReflectionBypass.java)
  - the classloader route and the method handles over it.
- [`ReflectedMembers`](src/main/java/kmlib/starsector/ui/coreui/ReflectedMembers.java)
  - the searches and the by-name reads, writes and calls.
- [`ParameterCompatibility`](src/main/java/kmlib/starsector/ui/coreui/ParameterCompatibility.java)
  - the rule deciding which member a caller's arguments reach.
- [`ReflectedField`](src/main/java/kmlib/starsector/ui/coreui/ReflectedField.java),
  [`ReflectedMethod`](src/main/java/kmlib/starsector/ui/coreui/ReflectedMethod.java) and
  [`ReflectedConstructor`](src/main/java/kmlib/starsector/ui/coreui/ReflectedConstructor.java)
  - one member each, described and callable without its own type being named.

Nothing else under that package derives from it.
`CoreUiTree`, `CoreUiMethods` and `CoreUiMethod` call into the facility and contain none of its code.
