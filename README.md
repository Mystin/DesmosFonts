## Desmos Fonts: Porting Typefaces into Desmos Graphing Calculator

### Preconditions
* Your desired typeface file has been converted to the .SVG file extension
* Your desired destination graph contains these functions:
  * b_{ezier2}\left(P_{0},\ P_{1},\ P_{2},\ t\right)=\left(1-t\right)^{2}P_{0}+2\left(1-t\right)tP_{1}+t^{2}P_{2}
  * b_{ezier3}\left(P_{0},\ P_{1},\ P_{2},\ P_{3},\ t\right)=\left(1-t\right)^{3}P_{0}+3\left(1-t\right)^{2}tP_{1}+3\left(1-t\right)t^{2}P_{2}+t^{3}P_{3}

### Notes
* The default name for the functions is g<sub>lyphA</sub> where A is the unicode character of the glyph. Desmos does not allow all characters in this subscript, so you may need to rename accordingly.
* I have no clue what I'm doing so please provide feedback if necessary.
