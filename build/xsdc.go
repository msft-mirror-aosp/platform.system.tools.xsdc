// Copyright 2018 Google Inc. All rights reserved.
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
	"path/filepath"

	"github.com/google/blueprint"
	"github.com/google/blueprint/proptools"

	"android/soong/android"
	"android/soong/java"
)

func init() {
	pctx.Import("android/soong/java/config")
	android.RegisterModuleType("xsd_config", xsdConfigFactory)

	android.PreArchMutators(func(ctx android.RegisterMutatorsContext) {
		ctx.TopDown("xsd_config", xsdConfigMutator).Parallel()
	})
}

var (
	pctx = android.NewPackageContext("android/xsdc")

	xsdc = pctx.HostBinToolVariable("xsdcCmd", "xsdc")
	xsdcRule = pctx.StaticRule("xsdcRule", blueprint.RuleParams{
		Command: `rm -rf "${out}.temp" && mkdir -p "${out}.temp" && ` +
			`${xsdcCmd} $in $pkgName ${out}.temp && ` +
			`${config.SoongZipCmd} -jar -o ${out} -C ${out}.temp -D ${out}.temp && ` +
			`rm -rf ${out}.temp`,
		Depfile:     "${out}.d",
		Deps:        blueprint.DepsGCC,
		CommandDeps: []string{"${xsdcCmd}", "${config.SoongZipCmd}"},
		Description: "xsdc Java ${in} => ${out}",
	}, "pkgName")
)

type xsdConfigProperties struct {
	Srcs []string
	Package_name *string
}

type xsdConfig struct {
	android.ModuleBase

	properties xsdConfigProperties

	genOutputDir android.Path
	genOutputs_j android.WritablePaths
	genOutputs_c android.WritablePaths
	genOutputs_h android.WritablePaths
}

type ApiToCheck struct {
	Api_file         *string
	Removed_api_file *string
	Args             *string
}

type CheckApi struct {
	Last_released ApiToCheck
	Current       ApiToCheck
}
type DroidstubsProperties struct {
	Name                 *string
	No_framework_libs    *bool
	Installable          *bool
	Srcs                 []string
	Args                 *string
	Api_filename         *string
	Removed_api_filename *string
	Check_api            CheckApi
}

func (module *xsdConfig) GeneratedSourceFiles() android.Paths {
	return module.genOutputs_c.Paths()
}

func (module *xsdConfig) Srcs() android.Paths {
	return module.genOutputs_j.Paths()
}

func (module *xsdConfig) GeneratedDeps() android.Paths {
	return module.genOutputs_h.Paths()
}

func (module *xsdConfig) GeneratedHeaderDirs() android.Paths {
	return android.Paths{module.genOutputDir}
}

func (module *xsdConfig) DepsMutator(ctx android.BottomUpMutatorContext) {
	// no need to implement
}

func (module *xsdConfig) GenerateAndroidBuildActions(ctx android.ModuleContext) {
	if len(module.properties.Srcs) != 1 {
		ctx.PropertyErrorf("srcs", "xsd_config must be one src")
	}

	xsdFile := module.properties.Srcs[0]
	pkgName := *module.properties.Package_name

	module.genOutputs_j = append(module.genOutputs_j, android.PathForModuleGen(ctx, "xsdcgen.srcjar"))

	ctx.Build(pctx, android.BuildParams{
		Rule: xsdcRule,
		Description:     "xsdc " + xsdFile,
		Input:           android.PathForModuleSrc(ctx, xsdFile),
		Output:          module.genOutputs_j[0],
		Args: map[string]string{
			"pkgName": pkgName,
		},
	})
}

func xsdConfigMutator(mctx android.TopDownMutatorContext) {
	if module, ok := mctx.Module().(*xsdConfig); ok {
		name := module.BaseModuleName()

		args := " --stub-packages " + *module.properties.Package_name +
			" --hide MissingPermission --hide BroadcastBehavior" +
			" --hide HiddenSuperclass --hide DeprecationMismatch --hide UnavailableSymbol" +
			" --hide SdkConstant --hide HiddenTypeParameter --hide Todo --hide Typo"

		currentApiFileName := filepath.Join("api", "current.txt")
		removedApiFileName := filepath.Join("api", "removed.txt")

		check_api := CheckApi{}

		check_api.Current.Api_file = proptools.StringPtr(currentApiFileName)
		check_api.Current.Removed_api_file = proptools.StringPtr(removedApiFileName)

		check_api.Last_released.Api_file = proptools.StringPtr(
			filepath.Join("api", "last_current.txt"))
		check_api.Last_released.Removed_api_file = proptools.StringPtr(
			filepath.Join("api", "last_removed.txt"))


		mctx.CreateModule(android.ModuleFactoryAdaptor(java.DroidstubsFactory), &DroidstubsProperties{
			Name:                 proptools.StringPtr(name + "-docs"),
			Srcs:                 []string{":" + name},
			Args:                 proptools.StringPtr(args),
			Api_filename:         proptools.StringPtr(currentApiFileName),
			Removed_api_filename: proptools.StringPtr(removedApiFileName),
			Check_api:            check_api,
			Installable:          proptools.BoolPtr(false),
			No_framework_libs:    proptools.BoolPtr(true),
		})
	}
}

func xsdConfigFactory() android.Module {
	module := &xsdConfig{}
	module.AddProperties(&module.properties)
	android.InitAndroidModule(module)

	return module
}

