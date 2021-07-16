.arch arvm7ve
.arch .text

.global	main
main:
.__BB__0:
@predBB:
	mov	%%0,	#2
	mov	%%1,	#3
	add	%%2,	%%0,	%%1
	mov	r0,	%%2
	b	lr


.data
.align 4
