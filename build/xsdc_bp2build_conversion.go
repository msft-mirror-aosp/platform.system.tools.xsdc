// Copyright 2023 Google Inc. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package xsdc

import (
	"github.com/google/blueprint/proptools"

	"android/soong/android"
	"android/soong/bazel"
)

type xsdFilegroupAttributes struct {
	Srcs bazel.LabelListAttribute
}

type xsdCcAttributes struct {
	Src                         bazel.LabelAttribute
	Include_files               bazel.LabelListAttribute
	Package_name                bazel.StringAttribute
	Gen_writer                  bazel.BoolAttribute
	Enums_only                  bazel.BoolAttribute
	Parser_only                 bazel.BoolAttribute
	Boolean_getter              bazel.BoolAttribute
	Tinyxml                     bazel.BoolAttribute
	Root_elements               bazel.StringListAttribute
	Deps                        bazel.LabelListAttribute
	Implementation_dynamic_deps bazel.LabelListAttribute
}

func (xsd *xsdConfig) bp2buildFilegroupTarget(ctx android.TopDownMutatorContext) {
	ctx.CreateBazelTargetModule(
		bazel.BazelTargetModuleProperties{
			Rule_class: "filegroup",
		},
		android.CommonAttributes{
			Name: xsd.Name(),
		},
		&xsdFilegroupAttributes{
			Srcs: bazel.MakeLabelListAttribute(
				android.BazelLabelForModuleSrc(ctx, append(xsd.properties.Srcs, xsd.properties.Include_files...)),
			),
		},
	)
}

func (xsd *xsdConfig) bp2buildCcTarget(ctx android.TopDownMutatorContext) {
	if len(xsd.properties.Srcs) != 1 {
		ctx.PropertyErrorf("srcs", "xsd_config must a single src. Got %v", xsd.properties.Srcs)
	}
	xsdFile := xsd.properties.Srcs[0]

	xmlLib := "libxml2"
	if proptools.Bool(xsd.properties.Tinyxml) {
		xmlLib = "libtinyxml2"
	}
	ctx.CreateBazelTargetModule(
		bazel.BazelTargetModuleProperties{
			Bzl_load_location: "//build/bazel/rules/cc:cc_xsd_config_library.bzl",
			Rule_class:        "cc_xsd_config_library",
		},
		android.CommonAttributes{
			Name: xsd.CppBp2buildTargetName(),
		},
		&xsdCcAttributes{
			Src: *bazel.MakeLabelAttribute(
				android.BazelLabelForModuleSrcSingle(ctx, xsdFile).Label,
			),
			Include_files: bazel.MakeLabelListAttribute(
				android.BazelLabelForModuleSrc(ctx, xsd.properties.Include_files),
			),
			Package_name: bazel.StringAttribute{
				Value: xsd.properties.Package_name,
			},
			Gen_writer: bazel.BoolAttribute{
				Value: xsd.properties.Gen_writer,
			},
			Enums_only: bazel.BoolAttribute{
				Value: xsd.properties.Enums_only,
			},
			Parser_only: bazel.BoolAttribute{
				Value: xsd.properties.Parser_only,
			},
			Boolean_getter: bazel.BoolAttribute{
				Value: xsd.properties.Boolean_getter,
			},
			Tinyxml: bazel.BoolAttribute{
				Value: xsd.properties.Tinyxml,
			},
			Root_elements: bazel.MakeStringListAttribute(
				xsd.properties.Root_elements,
			),
			// The generated cpp file includes additional .h files from xsdc.
			// This needs to be added to the deps so that we can compile the internal cc_static_library.
			// https://cs.android.com/android/_/android/platform/system/tools/xsdc/+/be3543328eb878d094870364333f1fd02f50ddfd:src/main/java/com/android/xsdc/cpp/CppCodeGenerator.java;l=171-174;drc=1da17e8ed45748c16a5c2bade198ae22fb411949;bpv=1;bpt=0
			Deps: bazel.MakeLabelListAttribute(
				android.BazelLabelForModuleDeps(ctx, []string{"libxsdc-utils"}),
			),
			Implementation_dynamic_deps: bazel.MakeLabelListAttribute(
				android.BazelLabelForModuleDeps(ctx, []string{xmlLib}),
			),
		},
	)

}

type xsdJavaAttributes struct {
	Src            bazel.LabelAttribute
	Sdk_version    bazel.StringAttribute
	Include_files  bazel.LabelListAttribute
	Package_name   bazel.StringAttribute
	Nullability    bazel.BoolAttribute
	Gen_has        bazel.BoolAttribute
	Gen_writer     bazel.BoolAttribute
	Boolean_getter bazel.BoolAttribute
	Root_elements  bazel.StringListAttribute
	Deps           bazel.LabelListAttribute
}

func (xsd *xsdConfig) bp2buildJavaTarget(ctx android.TopDownMutatorContext) {
	if len(xsd.properties.Srcs) != 1 {
		ctx.PropertyErrorf("srcs", "xsd_config must a single src. Got %v", xsd.properties.Srcs)
	}
	xsdFile := xsd.properties.Srcs[0]

	// The generated code depends on stub annotations if either
	// a. `nullability: true` in the xsd_config's Android.bp file
	// https://cs.android.com/search?q=xsd_config%20nullability.*true%20f:%5C.bp&sq=&ss=android%2Fplatform%2Fsuperproject
	// b. .xsd schema requests nullable annotation
	// https://cs.android.com/search?q=annotation.*nullable%20f:%5C.xsd&sq=&ss=android%2Fplatform%2Fsuperproject
	// bp2build does not have sufficient metadata about (b), therefore it aggressivesly adds stub-annotations as dep of all generated java_xsd_config_library targets.
	deps := bazel.MakeLabelListAttribute(
		android.BazelLabelForModuleDeps(ctx, []string{"stub-annotations"}),
	)
	// The generated code depends on org.xmlpull.v1.XmlPullParser.*
	// https://cs.android.com/android/_/android/platform/system/tools/xsdc/+/be3543328eb878d094870364333f1fd02f50ddfd:src/main/java/com/android/xsdc/java/JavaCodeGenerator.java;l=313;drc=1da17e8ed45748c16a5c2bade198ae22fb411949;bpv=0;bpt=0
	// For device libraries, this will come the android SDK
	// For host libraries, this will come from the `kxml` library
	deps.SetSelectValue(bazel.OsConfigurationAxis,
		android.Android.Name,
		// Device variant gets this dep from the android sdk (by using core_current)
		bazel.MakeLabelList(
			[]bazel.Label{},
		),
	)
	deps.SetSelectValue(bazel.OsConfigurationAxis,
		bazel.ConditionsDefaultConfigKey,
		// Version copied from this host java library that uses .java files generated from .xsd
		// https://cs.android.com/android/_/android/platform/system/tools/xsdc/+/be3543328eb878d094870364333f1fd02f50ddfd:tests/Android.bp;l=26;bpv=1;bpt=0;drc=7f84bff87550516b148dc03354363fbed0c5f62b
		android.BazelLabelForModuleDeps(ctx, []string{"kxml2-2.3.0"}),
	)

	ctx.CreateBazelTargetModule(
		bazel.BazelTargetModuleProperties{
			Bzl_load_location: "//build/bazel/rules/java:java_xsd_config_library.bzl",
			Rule_class:        "java_xsd_config_library",
		},
		android.CommonAttributes{
			Name: xsd.JavaBp2buildTargetName(),
		},
		&xsdJavaAttributes{
			Src: *bazel.MakeLabelAttribute(
				android.BazelLabelForModuleSrcSingle(ctx, xsdFile).Label,
			),
			Include_files: bazel.MakeLabelListAttribute(
				android.BazelLabelForModuleSrc(ctx, xsd.properties.Include_files),
			),
			Package_name: bazel.StringAttribute{
				Value: xsd.properties.Package_name,
			},
			Nullability: bazel.BoolAttribute{
				Value: xsd.properties.Nullability,
			},
			Gen_has: bazel.BoolAttribute{
				Value: xsd.properties.Gen_has,
			},
			Gen_writer: bazel.BoolAttribute{
				Value: xsd.properties.Gen_writer,
			},
			Boolean_getter: bazel.BoolAttribute{
				Value: xsd.properties.Boolean_getter,
			},
			Root_elements: bazel.MakeStringListAttribute(
				xsd.properties.Root_elements,
			),
			Deps: deps,
			// The android.jar corresponding to public, system, ... have package private versions of stub annotations
			// Since the .java generated from .xsd contains nullable annotations, it needs an sdk_version that does not contain package private versions of these classes
			// core api surface is one of them
			Sdk_version: bazel.StringAttribute{
				Value: proptools.StringPtr("core_current"),
			},
		},
	)
}

func (xsd *xsdConfig) ConvertWithBp2build(ctx android.TopDownMutatorContext) {
	xsd.bp2buildFilegroupTarget(ctx)
	xsd.bp2buildCcTarget(ctx)
	xsd.bp2buildJavaTarget(ctx)
}

// Returns the name of cc_xsd_config_library target created by bp2build.
func (xsd *xsdConfig) CppBp2buildTargetName() string {
	return xsd.Name() + "-cpp"
}

// Returns the name of java_xsd_config_library target created by bp2build.
func (xsd *xsdConfig) JavaBp2buildTargetName() string {
	return xsd.Name() + "-java"
}
