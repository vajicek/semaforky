/// Copyright (C) 2024, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

const uint8_t FixedWidthAbcdBitmaps[] PROGMEM = {
	// 'A, 21x32px
	0x00, 0xf8, 0x00, 0x00, 0xf8, 0x00, 0x01, 0xf8, 0x00, 0x01, 0xfc, 0x00, 0x01, 0xfc, 0x00, 0x01,
	0xdc, 0x00, 0x03, 0xde, 0x00, 0x03, 0xde, 0x00, 0x03, 0x8e, 0x00, 0x07, 0x8f, 0x00, 0x07, 0x8f,
	0x00, 0x07, 0x8f, 0x00, 0x07, 0x07, 0x00, 0x0f, 0x07, 0x80, 0x0f, 0x07, 0x80, 0x0f, 0x07, 0x80,
	0x0e, 0x03, 0x80, 0x1e, 0x03, 0xc0, 0x1e, 0x03, 0xc0, 0x1e, 0x03, 0xc0, 0x1f, 0xff, 0xc0, 0x3f,
	0xff, 0xe0, 0x3f, 0xff, 0xe0, 0x38, 0x01, 0xe0, 0x38, 0x00, 0xe0, 0x78, 0x00, 0xf0, 0x78, 0x00,
	0xf0, 0x78, 0x00, 0xf0, 0x78, 0x00, 0xf0, 0xf0, 0x00, 0xf0, 0xf0, 0x00, 0x78, 0xf0, 0x00, 0x78,

	// 'B, 21x32px
	0x1f, 0xf8, 0x00, 0x3f, 0xff, 0x00, 0x3f, 0xff, 0x80, 0x3f, 0xff, 0xc0, 0x3c, 0x0f, 0xc0, 0x3c,
	0x03, 0xe0, 0x3c, 0x01, 0xe0, 0x3c, 0x01, 0xe0, 0x3c, 0x01, 0xe0, 0x3c, 0x01, 0xe0, 0x3c, 0x01,
	0xe0, 0x3c, 0x03, 0xc0, 0x3c, 0x0f, 0x80, 0x3f, 0xff, 0x00, 0x3f, 0xfe, 0x00, 0x3f, 0xff, 0x80,
	0x3c, 0x07, 0xc0, 0x3c, 0x03, 0xe0, 0x3c, 0x01, 0xe0, 0x3c, 0x00, 0xf0, 0x3c, 0x00, 0xf0, 0x3c,
	0x00, 0xf0, 0x3c, 0x00, 0xf0, 0x3c, 0x00, 0xf0, 0x3c, 0x00, 0xf0, 0x3c, 0x01, 0xf0, 0x3c, 0x03,
	0xe0, 0x3c, 0x07, 0xe0, 0x3f, 0xff, 0xc0, 0x3f, 0xff, 0x80, 0x3f, 0xff, 0x00, 0x1f, 0xf8, 0x00,

	// 'C, 21x32px
	0x00, 0x3f, 0x80, 0x00, 0xff, 0xe0, 0x03, 0xff, 0xf0, 0x07, 0xff, 0xe0, 0x07, 0xe0, 0xe0, 0x0f,
	0x80, 0x20, 0x0f, 0x00, 0x00, 0x1e, 0x00, 0x00, 0x1e, 0x00, 0x00, 0x1e, 0x00, 0x00, 0x3e, 0x00,
	0x00, 0x3c, 0x00, 0x00, 0x3c, 0x00, 0x00, 0x3c, 0x00, 0x00, 0x3c, 0x00, 0x00, 0x3c, 0x00, 0x00,
	0x3c, 0x00, 0x00, 0x3c, 0x00, 0x00, 0x3c, 0x00, 0x00, 0x3c, 0x00, 0x00, 0x3c, 0x00, 0x00, 0x3e,
	0x00, 0x00, 0x1e, 0x00, 0x00, 0x1e, 0x00, 0x00, 0x1f, 0x00, 0x00, 0x0f, 0x00, 0x00, 0x0f, 0x80,
	0x00, 0x07, 0xe0, 0x60, 0x07, 0xff, 0xf0, 0x03, 0xff, 0xf0, 0x00, 0xff, 0xe0, 0x00, 0x3f, 0x80,

	// 'D, 21x32px
	0x1f, 0xf0, 0x00, 0x7f, 0xfe, 0x00, 0x7f, 0xff, 0x00, 0x7f, 0xff, 0x80, 0x78, 0x1f, 0xc0, 0x78,
	0x03, 0xe0, 0x78, 0x01, 0xe0, 0x78, 0x01, 0xf0, 0x78, 0x00, 0xf0, 0x78, 0x00, 0xf0, 0x78, 0x00,
	0xf8, 0x78, 0x00, 0x78, 0x78, 0x00, 0x78, 0x78, 0x00, 0x78, 0x78, 0x00, 0x78, 0x78, 0x00, 0x78,
	0x78, 0x00, 0x78, 0x78, 0x00, 0x78, 0x78, 0x00, 0x78, 0x78, 0x00, 0x78, 0x78, 0x00, 0x78, 0x78,
	0x00, 0xf8, 0x78, 0x00, 0xf0, 0x78, 0x00, 0xf0, 0x78, 0x01, 0xf0, 0x78, 0x01, 0xe0, 0x78, 0x03,
	0xe0, 0x78, 0x1f, 0xc0, 0x7f, 0xff, 0x80, 0x7f, 0xff, 0x00, 0x7f, 0xfe, 0x00, 0x3f, 0xf0, 0x00,
};

const GFXglyph FixedWidthAbcdGlyphs[] PROGMEM = {
	// Index,  W, H, xAdv, dX, dY
	{0*96, 24, 32, 21, 0, -32},   // 0x41 'A'
	{1*96, 24, 32, 21, 0, -32},   // 0x42 'B'
	{2*96, 24, 32, 21, 0, -32},   // 0x43 'C'
	{3*96, 24, 32, 21, 0, -32},   // 0x44 'D'
};

const GFXfont FixedWidthAbcd PROGMEM = {
	(uint8_t *)FixedWidthAbcdBitmaps,
	(GFXglyph *)FixedWidthAbcdGlyphs,
	0x41, 0x44, 32
};
