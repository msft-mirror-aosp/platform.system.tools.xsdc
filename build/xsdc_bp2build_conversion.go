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

func (xsd *xsdConfig) ConvertWithBp2build(ctx android.TopDownMutatorContext) {
	xsd.bp2buildFilegroupTarget(ctx)
	xsd.bp2buildCcTarget(ctx)
}

// Returns the name of cc_xsd_config_library target created by bp2build.
func (xsd *xsdConfig) CppBp2buildTargetName() string {
	return xsd.Name() + "-cpp"
}
