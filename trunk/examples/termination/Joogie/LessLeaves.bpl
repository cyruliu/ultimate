type ref;
type realVar;
type classConst;
// type Field x;
// var $HeapVar : <x>[ref, Field x]x;

const unique $null : ref ;
const unique $intArrNull : [int]int ;
const unique $realArrNull : [int]realVar ;
const unique $refArrNull : [int]ref ;

const unique $arrSizeIdx : int;
var $intArrSize : [int]int;
var $realArrSize : [realVar]int;
var $refArrSize : [ref]int;

var $stringSize : [ref]int;

//built-in axioms 
axiom ($arrSizeIdx == -1);

//note: new version doesn't put helpers in the perlude anymore//Prelude finished 



var Tree$Tree$right255 : Field ref;
var java.lang.String$lp$$rp$$Random$args257 : [int]ref;
var Tree$Tree$left254 : Field ref;
var int$Random$index0 : int;
var java.lang.Object$Tree$value256 : Field ref;


// procedure is generated by joogie.
function {:inline true} $neref(x : ref, y : ref) returns (__ret : int) {
if (x != y) then 1 else 0
}


// procedure is generated by joogie.
function {:inline true} $realarrtoref($param00 : [int]realVar) returns (__ret : ref);



// procedure is generated by joogie.
function {:inline true} $modreal($param00 : realVar, $param11 : realVar) returns (__ret : realVar);



	 //  @line: 14
// <Tree: Tree createNode()>
procedure Tree$Tree$createNode$2234() returns (__ret : ref)
  modifies $HeapVar;
 {
var $r230 : ref;
var $r128 : ref;
var r029 : ref;
	 //  @line: 15
Block43:
	 //  @line: 15
	$r128 := $newvariable((44));
	 assume ($neref(($newvariable((44))), ($null))==1);
	 assert ($neref(($r128), ($null))==1);
	 //  @line: 15
	 call void$Tree$$la$init$ra$$2233(($r128));
	 //  @line: 15
	r029 := $r128;
	 //  @line: 16
	$r230 := $newvariable((45));
	 assume ($neref(($newvariable((45))), ($null))==1);
	 assert ($neref(($r230), ($null))==1);
	 //  @line: 16
	 call void$java.lang.Object$$la$init$ra$$28(($r230));
	 assert ($neref((r029), ($null))==1);
	 //  @line: 16
	$HeapVar[r029, java.lang.Object$Tree$value256] := $r230;
	 //  @line: 17
	__ret := r029;
	 return;
}


// procedure is generated by joogie.
function {:inline true} $leref($param00 : ref, $param11 : ref) returns (__ret : int);



// <java.lang.String: int length()>
procedure int$java.lang.String$length$59(__this : ref) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $modint($param00 : int, $param11 : int) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $gtref($param00 : ref, $param11 : ref) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $eqrealarray($param00 : [int]realVar, $param11 : [int]realVar) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $addint(x : int, y : int) returns (__ret : int) {
(x + y)
}


// procedure is generated by joogie.
function {:inline true} $subref($param00 : ref, $param11 : ref) returns (__ret : ref);



// procedure is generated by joogie.
function {:inline true} $inttoreal($param00 : int) returns (__ret : realVar);



// procedure is generated by joogie.
function {:inline true} $shrint($param00 : int, $param11 : int) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $negReal($param00 : realVar) returns (__ret : int);



	 //  @line: 33
// <LessLeaves: boolean less_leaves(Tree,Tree)>
procedure boolean$LessLeaves$less_leaves$2231($param_0 : ref, $param_1 : ref) returns (__ret : int) {
var $r016 : ref;
var $r219 : ref;
var r422 : ref;
var $r320 : ref;
var r523 : ref;
var $r117 : ref;
Block29:
	r422 := $param_0;
	r523 := $param_1;
	 goto Block30;
	 //  @line: 34
Block30:
	 goto Block33, Block31;
	 //  @line: 34
Block33:
	 //  @line: 34
	 assume ($negInt(($eqref((r422), ($null))))==1);
	 goto Block34;
	 //  @line: 34
Block31:
	 assume ($eqref((r422), ($null))==1);
	 goto Block32;
	 //  @line: 40
Block34:
	 goto Block35, Block36;
	 //  @line: 39
Block32:
	 goto Block37, Block39;
	 //  @line: 40
Block35:
	 assume ($eqref((r523), ($null))==1);
	 goto Block32;
	 //  @line: 40
Block36:
	 //  @line: 40
	 assume ($negInt(($eqref((r523), ($null))))==1);
	 assert ($neref((r422), ($null))==1);
	 //  @line: 35
	$r117 := $HeapVar[r422, Tree$Tree$left254];
	 assert ($neref((r422), ($null))==1);
	 //  @line: 35
	$r016 := $HeapVar[r422, Tree$Tree$right255];
	 //  @line: 35
	 call r422 := Tree$LessLeaves$append$2230(($r117), ($r016));
	 assert ($neref((r523), ($null))==1);
	 //  @line: 36
	$r320 := $HeapVar[r523, Tree$Tree$left254];
	 assert ($neref((r523), ($null))==1);
	 //  @line: 36
	$r219 := $HeapVar[r523, Tree$Tree$right255];
	 //  @line: 36
	 call r523 := Tree$LessLeaves$append$2230(($r320), ($r219));
	 goto Block30;
	 //  @line: 39
Block37:
	 assume ($neref((r523), ($null))==1);
	 goto Block38;
	 //  @line: 39
Block39:
	 //  @line: 39
	 assume ($negInt(($neref((r523), ($null))))==1);
	 goto Block40;
	 //  @line: 40
Block38:
	 //  @line: 40
	__ret := 1;
	 return;
	 //  @line: 40
Block40:
	 //  @line: 40
	__ret := 0;
	 return;
}


// procedure is generated by joogie.
function {:inline true} $ushrint($param00 : int, $param11 : int) returns (__ret : int);



	 //  @line: 50
// <Tree: void main(java.lang.String[])>
procedure void$Tree$main$2236($param_0 : [int]ref)
  modifies java.lang.String$lp$$rp$$Random$args257, $stringSize;
 {
var r044 : [int]ref;

 //temp local variables 
var $freshlocal0 : ref;

Block71:
	r044 := $param_0;
	 //  @line: 51
	java.lang.String$lp$$rp$$Random$args257 := r044;
	 //  @line: 52
	 call $freshlocal0 := Tree$Tree$createTree$2235();
	 return;
}


// procedure is generated by joogie.
function {:inline true} $refarrtoref($param00 : [int]ref) returns (__ret : ref);



// procedure is generated by joogie.
function {:inline true} $divref($param00 : ref, $param11 : ref) returns (__ret : ref);



// procedure is generated by joogie.
function {:inline true} $mulref($param00 : ref, $param11 : ref) returns (__ret : ref);



// procedure is generated by joogie.
function {:inline true} $neint(x : int, y : int) returns (__ret : int) {
if (x != y) then 1 else 0
}


// procedure is generated by joogie.
function {:inline true} $ltreal($param00 : realVar, $param11 : realVar) returns (__ret : int);



	 //  @line: 5
// <LessLeaves: void main(java.lang.String[])>
procedure void$LessLeaves$main$2229($param_0 : [int]ref)
  modifies java.lang.String$lp$$rp$$Random$args257, $stringSize;
 {
var r26 : ref;
var r14 : ref;
var r02 : [int]ref;

 //temp local variables 
var $freshlocal0 : int;

Block17:
	r02 := $param_0;
	 //  @line: 6
	java.lang.String$lp$$rp$$Random$args257 := r02;
	 //  @line: 7
	 call r14 := Tree$Tree$createTree$2235();
	 //  @line: 8
	 call r26 := Tree$Tree$createTree$2235();
	 //  @line: 9
	 call $freshlocal0 := boolean$LessLeaves$less_leaves$2231((r14), (r26));
	 return;
}


// procedure is generated by joogie.
function {:inline true} $reftorefarr($param00 : ref) returns (__ret : [int]ref);



// procedure is generated by joogie.
function {:inline true} $gtint(x : int, y : int) returns (__ret : int) {
if (x > y) then 1 else 0
}


// procedure is generated by joogie.
function {:inline true} $reftoint($param00 : ref) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $addref($param00 : ref, $param11 : ref) returns (__ret : ref);



// procedure is generated by joogie.
function {:inline true} $xorreal($param00 : realVar, $param11 : realVar) returns (__ret : int);



// <LessLeaves: void <init>()>
procedure void$LessLeaves$$la$init$ra$$2228(__this : ref)  requires ($neref((__this), ($null))==1);
 {
var r01 : ref;
Block16:
	r01 := __this;
	 assert ($neref((r01), ($null))==1);
	 //  @line: 1
	 call void$java.lang.Object$$la$init$ra$$28((r01));
	 return;
}


// procedure is generated by joogie.
function {:inline true} $andref($param00 : ref, $param11 : ref) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $cmpreal(x : realVar, y : realVar) returns (__ret : int) {
if ($ltreal((x), (y)) == 1) then 1 else if ($eqreal((x), (y)) == 1) then 0 else -1
}


// procedure is generated by joogie.
function {:inline true} $addreal($param00 : realVar, $param11 : realVar) returns (__ret : realVar);



// procedure is generated by joogie.
function {:inline true} $gtreal($param00 : realVar, $param11 : realVar) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $eqreal(x : realVar, y : realVar) returns (__ret : int) {
if (x == y) then 1 else 0
}


// procedure is generated by joogie.
function {:inline true} $ltint(x : int, y : int) returns (__ret : int) {
if (x < y) then 1 else 0
}


// procedure is generated by joogie.
function {:inline true} $newvariable($param00 : int) returns (__ret : ref);



	 //  @line: 20
// <Tree: Tree createTree()>
procedure Tree$Tree$createTree$2235() returns (__ret : ref)
  modifies $HeapVar;
 {
var r034 : ref;
var $r441 : ref;
var i037 : int;
var $r138 : ref;
var i142 : int;
var $r239 : ref;
var r543 : ref;
var $r340 : ref;
	 //  @line: 21
Block46:
	 //  @line: 21
	 call i142 := int$Random$random$2238();
	 goto Block47;
	 //  @line: 22
Block47:
	 goto Block48, Block50;
	 //  @line: 22
Block48:
	 assume ($neint((i142), (0))==1);
	 goto Block49;
	 //  @line: 22
Block50:
	 //  @line: 22
	 assume ($negInt(($neint((i142), (0))))==1);
	 //  @line: 23
	__ret := $null;
	 return;
	 //  @line: 25
Block49:
	 //  @line: 25
	 call r034 := Tree$Tree$createNode$2234();
	 goto Block51;
	 //  @line: 26
Block51:
	 //  @line: 26
	r543 := r034;
	 goto Block52;
	 //  @line: 28
Block52:
	 goto Block55, Block53;
	 //  @line: 28
Block55:
	 //  @line: 28
	 assume ($negInt(($leint((i142), (0))))==1);
	 //  @line: 29
	 call i037 := int$Random$random$2238();
	 goto Block56;
	 //  @line: 28
Block53:
	 assume ($leint((i142), (0))==1);
	 goto Block54;
	 //  @line: 30
Block56:
	 goto Block59, Block57;
	 //  @line: 48
Block54:
	 //  @line: 48
	__ret := r034;
	 return;
	 //  @line: 30
Block59:
	 //  @line: 30
	 assume ($negInt(($leint((i037), (0))))==1);
	 assert ($neref((r543), ($null))==1);
	 //  @line: 31
	$r340 := $HeapVar[r543, Tree$Tree$left254];
	 goto Block60;
	 //  @line: 30
Block57:
	 assume ($leint((i037), (0))==1);
	 goto Block58;
	 //  @line: 31
Block60:
	 goto Block63, Block61;
	 //  @line: 38
Block58:
	 assert ($neref((r543), ($null))==1);
	 //  @line: 38
	$r138 := $HeapVar[r543, Tree$Tree$right255];
	 goto Block66;
	 //  @line: 31
Block63:
	 //  @line: 31
	 assume ($negInt(($neref(($r340), ($null))))==1);
	 //  @line: 32
	 call $r441 := Tree$Tree$createNode$2234();
	 assert ($neref((r543), ($null))==1);
	 //  @line: 32
	$HeapVar[r543, Tree$Tree$left254] := $r441;
	 //  @line: 33
	r543 := r034;
	 goto Block64;
	 //  @line: 31
Block61:
	 assume ($neref(($r340), ($null))==1);
	 goto Block62;
	 //  @line: 38
Block66:
	 goto Block67, Block69;
	 //  @line: 45
Block64:
	 //  @line: 45
	i142 := $addint((i142), (-1));
	 goto Block70;
	 //  @line: 35
Block62:
	 assert ($neref((r543), ($null))==1);
	 //  @line: 35
	r543 := $HeapVar[r543, Tree$Tree$left254];
	 goto Block65;
	 //  @line: 38
Block67:
	 assume ($neref(($r138), ($null))==1);
	 goto Block68;
	 //  @line: 38
Block69:
	 //  @line: 38
	 assume ($negInt(($neref(($r138), ($null))))==1);
	 //  @line: 39
	 call $r239 := Tree$Tree$createNode$2234();
	 assert ($neref((r543), ($null))==1);
	 //  @line: 39
	$HeapVar[r543, Tree$Tree$right255] := $r239;
	 //  @line: 40
	r543 := r034;
	 goto Block64;
	 //  @line: 46
Block70:
	 goto Block52;
	 //  @line: 35
Block65:
	 goto Block64;
	 //  @line: 42
Block68:
	 assert ($neref((r543), ($null))==1);
	 //  @line: 42
	r543 := $HeapVar[r543, Tree$Tree$right255];
	 goto Block64;
}


// procedure is generated by joogie.
function {:inline true} $divint($param00 : int, $param11 : int) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $geint(x : int, y : int) returns (__ret : int) {
if (x >= y) then 1 else 0
}


// procedure is generated by joogie.
function {:inline true} $mulint($param00 : int, $param11 : int) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $leint(x : int, y : int) returns (__ret : int) {
if (x <= y) then 1 else 0
}


// procedure is generated by joogie.
function {:inline true} $shlref($param00 : ref, $param11 : ref) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $eqrefarray($param00 : [int]ref, $param11 : [int]ref) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $reftointarr($param00 : ref) returns (__ret : [int]int);



// procedure is generated by joogie.
function {:inline true} $ltref($param00 : ref, $param11 : ref) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $mulreal($param00 : realVar, $param11 : realVar) returns (__ret : realVar);



	 //  @line: 17
// <LessLeaves: Tree append(Tree,Tree)>
procedure Tree$LessLeaves$append$2230($param_0 : ref, $param_1 : ref) returns (__ret : ref)
  modifies $HeapVar;
 {
var $r211 : ref;
var r112 : ref;
var r313 : ref;
var r09 : ref;
Block18:
	r09 := $param_0;
	r112 := $param_1;
	 goto Block19;
	 //  @line: 18
Block19:
	 goto Block22, Block20;
	 //  @line: 18
Block22:
	 //  @line: 18
	 assume ($negInt(($neref((r09), ($null))))==1);
	 //  @line: 22
	__ret := r112;
	 return;
	 //  @line: 18
Block20:
	 assume ($neref((r09), ($null))==1);
	 goto Block21;
	 //  @line: 20
Block21:
	 //  @line: 20
	r313 := r09;
	 goto Block23;
	 //  @line: 22
Block23:
	 assert ($neref((r313), ($null))==1);
	 //  @line: 22
	$r211 := $HeapVar[r313, Tree$Tree$right255];
	 goto Block24;
	 //  @line: 22
Block24:
	 goto Block27, Block25;
	 //  @line: 22
Block27:
	 //  @line: 22
	 assume ($negInt(($eqref(($r211), ($null))))==1);
	 assert ($neref((r313), ($null))==1);
	 //  @line: 23
	r313 := $HeapVar[r313, Tree$Tree$right255];
	 goto Block23;
	 //  @line: 22
Block25:
	 assume ($eqref(($r211), ($null))==1);
	 goto Block26;
	 //  @line: 26
Block26:
	 assert ($neref((r313), ($null))==1);
	 //  @line: 26
	$HeapVar[r313, Tree$Tree$right255] := r112;
	 goto Block28;
	 //  @line: 27
Block28:
	 //  @line: 27
	__ret := r09;
	 return;
}


// procedure is generated by joogie.
function {:inline true} $shrref($param00 : ref, $param11 : ref) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $ushrreal($param00 : realVar, $param11 : realVar) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $shrreal($param00 : realVar, $param11 : realVar) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $divreal($param00 : realVar, $param11 : realVar) returns (__ret : realVar);



// procedure is generated by joogie.
function {:inline true} $orint($param00 : int, $param11 : int) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $reftorealarr($param00 : ref) returns (__ret : [int]realVar);



// procedure is generated by joogie.
function {:inline true} $cmpref(x : ref, y : ref) returns (__ret : int) {
if ($ltref((x), (y)) == 1) then 1 else if ($eqref((x), (y)) == 1) then 0 else -1
}


// <Random: void <init>()>
procedure void$Random$$la$init$ra$$2237(__this : ref)  requires ($neref((__this), ($null))==1);
 {
var r046 : ref;
Block72:
	r046 := __this;
	 assert ($neref((r046), ($null))==1);
	 //  @line: 1
	 call void$java.lang.Object$$la$init$ra$$28((r046));
	 return;
}


	 //  @line: 5
// <Random: int random()>
procedure int$Random$random$2238() returns (__ret : int)
  modifies $stringSize, int$Random$index0;
 {
var $i352 : int;
var $i150 : int;
var $i047 : int;
var r049 : ref;
var $i251 : int;
var $r148 : [int]ref;
	 //  @line: 6
Block73:
	 //  @line: 6
	$r148 := java.lang.String$lp$$rp$$Random$args257;
	 //  @line: 6
	$i047 := int$Random$index0;
	 assert ($geint(($i047), (0))==1);
	 assert ($ltint(($i047), ($refArrSize[$r148[$arrSizeIdx]]))==1);
	 //  @line: 6
	r049 := $r148[$i047];
	 //  @line: 7
	$i150 := int$Random$index0;
	 //  @line: 7
	$i251 := $addint(($i150), (1));
	 //  @line: 7
	int$Random$index0 := $i251;
	$i352 := $stringSize[r049];
	 //  @line: 8
	__ret := $i352;
	 return;
}


	 //  @line: 2
// <Random: void <clinit>()>
procedure void$Random$$la$clinit$ra$$2239()
  modifies int$Random$index0;
 {
	 //  @line: 3
Block74:
	 //  @line: 3
	int$Random$index0 := 0;
	 return;
}


// procedure is generated by joogie.
function {:inline true} $realtoint($param00 : realVar) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $geref($param00 : ref, $param11 : ref) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $orreal($param00 : realVar, $param11 : realVar) returns (__ret : int);



// <java.lang.Object: void <init>()>
procedure void$java.lang.Object$$la$init$ra$$28(__this : ref);



// procedure is generated by joogie.
function {:inline true} $eqint(x : int, y : int) returns (__ret : int) {
if (x == y) then 1 else 0
}


// procedure is generated by joogie.
function {:inline true} $ushrref($param00 : ref, $param11 : ref) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $modref($param00 : ref, $param11 : ref) returns (__ret : ref);



// procedure is generated by joogie.
function {:inline true} $eqintarray($param00 : [int]int, $param11 : [int]int) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $negRef($param00 : ref) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $lereal($param00 : realVar, $param11 : realVar) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $nereal(x : realVar, y : realVar) returns (__ret : int) {
if (x != y) then 1 else 0
}


// procedure is generated by joogie.
function {:inline true} $instanceof($param00 : ref, $param11 : classConst) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $xorref($param00 : ref, $param11 : ref) returns (__ret : int);



	 //  @line: 5
// <Tree: void <init>(Tree,Tree)>
procedure void$Tree$$la$init$ra$$2232(__this : ref, $param_0 : ref, $param_1 : ref)
  modifies $HeapVar;
  requires ($neref((__this), ($null))==1);
 {
var r024 : ref;
var r125 : ref;
var r226 : ref;
Block41:
	r024 := __this;
	r125 := $param_0;
	r226 := $param_1;
	 assert ($neref((r024), ($null))==1);
	 //  @line: 6
	 call void$java.lang.Object$$la$init$ra$$28((r024));
	 assert ($neref((r024), ($null))==1);
	 //  @line: 7
	$HeapVar[r024, Tree$Tree$left254] := r125;
	 assert ($neref((r024), ($null))==1);
	 //  @line: 8
	$HeapVar[r024, Tree$Tree$right255] := r226;
	 return;
}


// procedure is generated by joogie.
function {:inline true} $orref($param00 : ref, $param11 : ref) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $intarrtoref($param00 : [int]int) returns (__ret : ref);



// procedure is generated by joogie.
function {:inline true} $subreal($param00 : realVar, $param11 : realVar) returns (__ret : realVar);



// procedure is generated by joogie.
function {:inline true} $shlreal($param00 : realVar, $param11 : realVar) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $negInt(x : int) returns (__ret : int) {
if (x == 0) then 1 else 0
}


// procedure is generated by joogie.
function {:inline true} $gereal($param00 : realVar, $param11 : realVar) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $eqref(x : ref, y : ref) returns (__ret : int) {
if (x == y) then 1 else 0
}


// procedure is generated by joogie.
function {:inline true} $cmpint(x : int, y : int) returns (__ret : int) {
if (x < y) then 1 else if (x == y) then 0 else -1
}


// procedure is generated by joogie.
function {:inline true} $andint($param00 : int, $param11 : int) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $andreal($param00 : realVar, $param11 : realVar) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $shlint($param00 : int, $param11 : int) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $xorint($param00 : int, $param11 : int) returns (__ret : int);



	 //  @line: 10
// <Tree: void <init>()>
procedure void$Tree$$la$init$ra$$2233(__this : ref)  requires ($neref((__this), ($null))==1);
 {
var r027 : ref;
Block42:
	r027 := __this;
	 assert ($neref((r027), ($null))==1);
	 //  @line: 11
	 call void$java.lang.Object$$la$init$ra$$28((r027));
	 return;
}


// procedure is generated by joogie.
function {:inline true} $subint(x : int, y : int) returns (__ret : int) {
(x - y)
}


