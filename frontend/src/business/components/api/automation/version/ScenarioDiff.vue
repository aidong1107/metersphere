<template>
  <div class="compare-class">
    <el-card style="width: 50%;" ref="old">
      <el-card>
        <div class="card-content">
          <div class="ms-main-div" @click="showAll" >

            <!--操作按钮-->
            <div class="ms-opt-btn">
              <el-tooltip :content="$t('commons.follow')" placement="bottom" effect="dark" v-if="!showFollow">
                <i class="el-icon-star-off" style="color: #783987; font-size: 25px; margin-right: 5px;cursor: pointer;position: relative; top: 5px; " />
              </el-tooltip>
              <el-tooltip :content="$t('commons.cancel')" placement="bottom" effect="dark" v-if="showFollow">
                <i class="el-icon-star-on" style="color: #783987; font-size: 28px; margin-right: 5px;cursor: pointer;position: relative; top: 5px; " />
              </el-tooltip>
            </div>

            <div class="tip">{{ $t('test_track.plan_view.base_info') }}</div>
            <el-form :model="oldData" label-position="right" label-width="80px" size="small" :rules="rules"
                     ref="currentScenario" style="margin-right: 20px">
              <!-- 基础信息 -->
              <el-row>
                <el-col :span="7">
                  <el-form-item :label="$t('commons.name')" prop="name">
                    <el-input class="ms-scenario-input" size="small" v-model="oldData.name"/>
                  </el-form-item>
                </el-col>
                <el-col :span="7">
                  <el-form-item :label="$t('test_track.module.module')" prop="apiScenarioModuleId">
                    <ms-select-tree size="small" :data="moduleOptions" :defaultKey="oldData.apiScenarioModuleId" :obj="moduleObj" clearable checkStrictly/>
                  </el-form-item>
                </el-col>
                <el-col :span="7">
                  <el-form-item :label="$t('commons.status')" prop="status">
                    <el-select class="ms-scenario-input" size="small" v-model="oldData.status">
                      <el-option v-for="item in options" :key="item.id" :label="$t(item.label)" :value="item.id"/>
                    </el-select>
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row>
                <el-col :span="7">
                  <el-form-item :label="$t('api_test.definition.request.responsible')" prop="principal">
                    <el-select v-model="oldData.principal"
                               :placeholder="$t('api_test.definition.request.responsible')" filterable size="small"
                               class="ms-scenario-input">
                      <el-option
                        v-for="item in maintainerOptions"
                        :key="item.id"
                        :label="item.name + ' (' + item.id + ')'"
                        :value="item.id">
                      </el-option>
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="7">
                  <el-form-item :label="$t('test_track.case.priority')" prop="level">
                    <el-select class="ms-scenario-input" size="small" v-model="oldData.level">
                      <el-option v-for="item in levels" :key="item.id" :label="item.label" :value="item.id"/>
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="7">
                  <el-form-item :label="$t('api_test.automation.tag')" prop="tags">
                    <ms-input-tag :currentScenario="oldData" ref="tag"/>
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row>
                <el-col :span="7">
                  <el-form-item :label="$t('commons.description')" prop="description">
                    <el-input class="ms-http-textarea"
                              v-model="oldData.description"
                              type="textarea"
                              :autosize="{ minRows: 1, maxRows: 10}"
                              :rows="1" size="small"/>
                  </el-form-item>
                </el-col>
                <el-col :span="7" v-if="customNum">
                  <el-form-item label="ID" prop="customNum">
                    <el-input v-model.trim="oldData.customNum" size="small"></el-input>
                  </el-form-item>
                </el-col>
              </el-row>

            </el-form>
          </div>
          <!-- 场景步骤-->
          <div v-loading="loading">
            <div @click="showAll">
              <p class="tip">{{ $t('api_test.automation.scenario_step') }} </p>
            </div>
            <el-row>
              <el-col :span="21">
                <!-- 调试部分 -->
                <div class="ms-debug-div" @click="showAll" :class="{'is-top' : isTop}" ref="debugHeader">
                  <el-row style="margin: 5px">
                    <el-col :span="4" class="ms-col-one ms-font">
                      <el-tooltip placement="top" effect="light">
                        <template v-slot:content>
                          <div>{{
                              oldData.name === undefined || '' ? $t('api_test.scenario.name') : oldData.name
                            }}
                          </div>
                        </template>
                        <span class="scenario-name">
                        {{
                            oldData.name === undefined || '' ? $t('api_test.scenario.name') : oldData.name
                          }}
                    </span>
                      </el-tooltip>
                    </el-col>
                    <el-col :span="3" class="ms-col-one ms-font">
                      {{ $t('api_test.automation.step_total') }}：{{ 0}}
                    </el-col>
                    <el-col :span="3" class="ms-col-one ms-font">
                      <el-link class="head">{{ $t('api_test.automation.scenario_total') }}
                      </el-link>
                      ：{{ oldVariableSize }}
                    </el-col>
                    <el-col :span="3" class="ms-col-one ms-font">
                      <el-checkbox v-model="oldEnableCookieShare"><span style="font-size: 13px;">{{ $t('api_test.scenario.share_cookie') }}</span></el-checkbox>
                    </el-col>
                    <el-col :span="3" class="ms-col-one ms-font">
                      <el-checkbox v-model="oldOnSampleError"><span style="font-size: 13px;">{{ $t('commons.failure_continues') }}</span></el-checkbox>
                    </el-col>

                  </el-row>
                </div>

                <!-- 场景步骤内容 -->
                <div ref="stepInfo">
                  <el-tooltip :content="$t('api_test.automation.open_expansion')" placement="top" effect="light">
                    <i class="el-icon-circle-plus-outline ms-open-btn ms-open-btn-left"  @click="openExpansion('old')"/>
                  </el-tooltip>
                  <el-tooltip :content="$t('api_test.automation.close_expansion')" placement="top" effect="light">
                    <i class="el-icon-remove-outline ms-open-btn" size="mini"  @click="closeExpansion('old')"/>
                  </el-tooltip>
                  <el-tree node-key="resourceId" :props="props" :data="oldScenarioDefinition" class="ms-tree"
                           :default-expanded-keys="oldExpandedNode"
                           :expand-on-click-node="false"
                           highlight-current
                           @node-expand="nodeExpand(oldScenarioDefinition,null,'old')"
                           @node-collapse="nodeCollapse(oldScenarioDefinition,null,'old')"
                           draggable ref="stepTree">
                    <span class="custom-tree-node father" slot-scope="{ node, data}" style="width: 96%">
                      <!-- 步骤组件-->
                       <ms-component-config
                         :type="data.type"
                         :scenario="data"
                         :node="node"
                       />
                    </span>
                  </el-tree>
                </div>
              </el-col>
            </el-row>
          </div>
          <el-backtop target=".card-content" :visibility-height="100" :right="50"></el-backtop>
        </div>
      </el-card>
    </el-card>
    <el-card style="width: 50%;" ref="new">
      <el-card>
        <div class="card-content">
          <div class="ms-main-div" @click="showAll" v-if="type!=='detail'">

            <!--操作按钮-->
            <div class="ms-opt-btn">
              <el-tooltip :content="$t('commons.follow')" placement="bottom" effect="dark" v-if="!newShowFollow">
                <i class="el-icon-star-off" style="color: #783987; font-size: 25px; margin-right: 5px;cursor: pointer;position: relative; top: 5px; " />
              </el-tooltip>
              <el-tooltip :content="$t('commons.cancel')" placement="bottom" effect="dark" v-if="newShowFollow">
                <i class="el-icon-star-on" style="color: #783987; font-size: 28px; margin-right: 5px;cursor: pointer;position: relative; top: 5px; " />
              </el-tooltip>
            </div>

            <div class="tip">{{ $t('test_track.plan_view.base_info') }}</div>
            <el-form :model="newData" label-position="right" label-width="80px" size="small" :rules="rules"
                     ref="currentScenario" style="margin-right: 20px">
              <!-- 基础信息 -->
              <el-row>
                <el-col :span="7">
                  <el-form-item :label="$t('commons.name')" prop="name">
                    <el-input class="ms-scenario-input" size="small" v-model="newData.name"/>
                  </el-form-item>
                </el-col>
                <el-col :span="7">
                  <el-form-item :label="$t('test_track.module.module')" prop="apiScenarioModuleId">
                    <ms-select-tree size="small" :data="moduleOptions" :defaultKey="newData.apiScenarioModuleId" :obj="moduleObj" clearable checkStrictly/>
                  </el-form-item>
                </el-col>
                <el-col :span="7">
                  <el-form-item :label="$t('commons.status')" prop="status">
                    <el-select class="ms-scenario-input" size="small" v-model="newData.status">
                      <el-option v-for="item in options" :key="item.id" :label="$t(item.label)" :value="item.id"/>
                    </el-select>
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row>
                <el-col :span="7">
                  <el-form-item :label="$t('api_test.definition.request.responsible')" prop="principal">
                    <el-select v-model="newData.principal"
                               :placeholder="$t('api_test.definition.request.responsible')" filterable size="small"
                               class="ms-scenario-input">
                      <el-option
                        v-for="item in maintainerOptions"
                        :key="item.id"
                        :label="item.name + ' (' + item.id + ')'"
                        :value="item.id">
                      </el-option>
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="7">
                  <el-form-item :label="$t('test_track.case.priority')" prop="level">
                    <el-select class="ms-scenario-input" size="small" v-model="newData.level">
                      <el-option v-for="item in levels" :key="item.id" :label="item.label" :value="item.id"/>
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="7">
                  <el-form-item :label="$t('api_test.automation.tag')" prop="tags">
                    <ms-input-tag :currentScenario="newData" ref="tag"/>
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row>
                <el-col :span="7">
                  <el-form-item :label="$t('commons.description')" prop="description">
                    <el-input class="ms-http-textarea"
                              v-model="newData.description"
                              type="textarea"
                              :autosize="{ minRows: 1, maxRows: 10}"
                              :rows="1" size="small"/>
                  </el-form-item>
                </el-col>
                <el-col :span="7" v-if="customNum">
                  <el-form-item label="ID" prop="customNum">
                    <el-input v-model.trim="newData.customNum" size="small"></el-input>
                  </el-form-item>
                </el-col>
              </el-row>

            </el-form>
          </div>
          <!-- 场景步骤-->
          <div v-loading="loading">
            <div @click="showAll">
              <p class="tip">{{ $t('api_test.automation.scenario_step') }} </p>
            </div>
            <el-row>
              <el-col :span="21">
                <!-- 调试部分 -->
                <div class="ms-debug-div" @click="showAll" :class="{'is-top' : isTop}" ref="debugHeader">
                  <el-row style="margin: 5px">
                    <el-col :span="4" class="ms-col-one ms-font">
                      <el-tooltip placement="top" effect="light">
                        <template v-slot:content>
                          <div>{{
                              newData.name === undefined || '' ? $t('api_test.scenario.name') : newData.name
                            }}
                          </div>
                        </template>
                        <span class="scenario-name">
                        {{
                            newData.name === undefined || '' ? $t('api_test.scenario.name') : newData.name
                          }}
                    </span>
                      </el-tooltip>
                    </el-col>
                    <el-col :span="3" class="ms-col-one ms-font">
                      {{ $t('api_test.automation.step_total') }}：{{ 0 }}
                    </el-col>
                    <el-col :span="3" class="ms-col-one ms-font">
                      <el-link class="head" >{{ $t('api_test.automation.scenario_total') }}
                      </el-link>
                      ：{{ newVariableSize }}
                    </el-col>
                    <el-col :span="3" class="ms-col-one ms-font">
                      <el-checkbox v-model="newEnableCookieShare"><span style="font-size: 13px;">{{ $t('api_test.scenario.share_cookie') }}</span></el-checkbox>
                    </el-col>
                    <el-col :span="3" class="ms-col-one ms-font">
                      <el-checkbox v-model="newOnSampleError"><span style="font-size: 13px;">{{ $t('commons.failure_continues') }}</span></el-checkbox>
                    </el-col>

                  </el-row>
                </div>

                <!-- 场景步骤内容 -->
                <div ref="stepInfo">
                  <el-tooltip :content="$t('api_test.automation.open_expansion')" placement="top" effect="light">
                    <i class="el-icon-circle-plus-outline ms-open-btn ms-open-btn-left"  @click="openExpansion('new')"/>
                  </el-tooltip>
                  <el-tooltip :content="$t('api_test.automation.close_expansion')" placement="top" effect="light">
                    <i class="el-icon-remove-outline ms-open-btn" size="mini" @click="closeExpansion('new')"/>
                  </el-tooltip>
                  <el-tree node-key="resourceId" :props="props" :data="newScenarioDefinition" class="ms-tree"
                           :default-expanded-keys="newExpandedNode"
                           :expand-on-click-node="false"
                           highlight-current
                           @node-expand="nodeExpand"
                           @node-collapse="nodeCollapse"
                          draggable ref="stepTree" >
                    <span class="custom-tree-node father" slot-scope="{ node, data}" style="width: 96%">
                      <!-- 步骤组件-->
                       <ms-component-config
                         :type="data.type"
                         :scenario="data"
                         :node="node"
                      />
                    </span>
                  </el-tree>
                </div>
              </el-col>
            </el-row>
          </div>

          <el-backtop target=".card-content" :visibility-height="100" :right="50"></el-backtop>
        </div>
      </el-card>
    </el-card>
    <button @click="getDiff"></button>
  </div>
</template>
<script>

import {API_STATUS, PRIORITY} from "@/business/components/api/definition/model/JsonData";
import {ENV_TYPE} from "@/common/js/constants";
import {ELEMENT_TYPE, STEP, TYPE_TO_C} from "@/business/components/api/automation/scenario/Setting";
import MsComponentConfig from "@/business/components/api/automation/scenario/component/ComponentConfig";
const {diff} = require("@/business/components/performance/v_node_diff");
const {KeyValue} = require("@/business/components/api/definition/model/ApiTestModel");
const {getUUID} = require("@/common/js/utils");
export default{
  name:"ScenarioDiff",
  props:{
    oldData:{
      type:Object
    },
    newData:{
      type:Object
    },
    showFollow:{
      type:Boolean
    },
    newShowFollow:{
      type:Boolean
    },
    customNum: {
      type: Boolean,
      default: false
    },
    isTop:{
      type: Boolean,
      default: false
    },
    rules:{
      type:Object
    },
    projectIds:{},
    newScenarioDefinition:{
      type: Array,
    },
    oldScenarioDefinition:{
      type: Array,
    },
    oldVariableSize:{},
    newVariableSize:{},
    moduleOptions:{},
    maintainerOptions:{},
    newEnableCookieShare:{},
    oldEnableCookieShare:{},
    oldOnSampleError:{},
    newOnSampleError:{},
    oldProjectEnvMap:{},
    newProjectEnvMap:{},
    type:{}
  },
  components:{
    MsComponentConfig,
    MsSelectTree: () => import("@/business/components/common/select-tree/SelectTree"),
    MsInputTag: () => import("@/business/components/api/automation/scenario/MsInputTag"),
    EnvPopover: () => import("@/business/components/api/automation/scenario/EnvPopover"),
  },
  data(){
   return{
     options: API_STATUS,
     levels: PRIORITY,
     loading: false,
     moduleObj: {
       id: 'id',
       label: 'name',
     },
     oldEnvResult:{
       loading: false
     },
     newEnvResult:{
       loading: false
     },
     showHideTree: true,
     environmentType: ENV_TYPE.JSON,
     projectList:[],
     props: {
       label: "label",
       children: "hashTree"
     },
     oldExpandedNode:[],
     newExpandedNode:[],
     stepEnable:true,



   }
  },
  methods:{
    getDiff(){
      let oldVnode = this.$refs.old
      let vnode = this.$refs.new
      //oldVnode.style.backgroundColor = "rgb(241,200,196)";
      console.log(this.$refs.old)
      console.log(this.$refs.new)
      diff(oldVnode,vnode);
    },
    showAll() {
      // 控制当有弹出页面操作时禁止刷新按钮列表
      if (!this.customizeVisible && !this.isBtnHide) {
        this.operatingElements = this.stepFilter.get("ALL");
        this.selectedTreeNode = undefined;
        this.$store.state.selectStep = undefined;
      }
    },
    oldShowPopover(){
      let definition = JSON.parse(JSON.stringify(this.oldData));
      definition.hashTree = this.oldScenarioDefinition;
      this.oldEnvResult.loading = true;
      this.getEnv(JSON.stringify(definition)).then(() => {
        this.$refs.envPopover.openEnvSelect();
        this.oldEnvResult.loading = false;
      })
    },
    newShowPopover(){
      let definition = JSON.parse(JSON.stringify(this.newData));
      definition.hashTree = this.newScenarioDefinition;
      this.newEnvResult.loading = true;
      this.getEnv(JSON.stringify(definition)).then(() => {
        this.$refs.envPopover.openEnvSelect();
        this.newEnvResult.loading = false;
      })
    },
    getWsProjects() {
      this.$get("/project/listAll", res => {
        this.projectList = res.data;
      })
    },
    changeNodeStatus(nodes,source) {
      for (let i in nodes) {
        if (nodes[i]) {
          if (this.expandedStatus) {
            if(source==="new"){
              this.newExpandedNode.push(nodes[i].resourceId);
            }else {
              this.oldExpandedNode.push(nodes[i].resourceId);
            }
          }
          nodes[i].active = this.expandedStatus;
          if (this.stepSize > 35 && this.expandedStatus) {
            nodes[i].active = false;
          }
          if (nodes[i].hashTree !== undefined && nodes[i].hashTree.length > 0) {
            this.changeNodeStatus(nodes[i].hashTree);
          }
        }
      }
    },
    openExpansion(source) {
      this.expandedNode = [];
      this.expandedStatus = true;
      let scenarioDefinition;
      if(source==="new"){
        scenarioDefinition = this.newScenarioDefinition;
      }else {
        scenarioDefinition = this.oldScenarioDefinition;
      }
      this.changeNodeStatus(scenarioDefinition,source);
    },
    closeExpansion(source) {
      this.expandedStatus = false;
      this.expandedNode = [];
      let scenarioDefinition;
      if(source==="new"){
        scenarioDefinition = this.newScenarioDefinition;
      }else {
        scenarioDefinition = this.oldScenarioDefinition;
      }
      this.changeNodeStatus(scenarioDefinition,source);
      this.showHide();
    },
    showHide() {
      this.showHideTree = false
      this.$nextTick(() => {
        this.showHideTree = true
      });
    },
    stepStatus(nodes) {
      for (let i in nodes) {
        if (nodes[i]) {
          nodes[i].enable = this.stepEnable;
          if (nodes[i].hashTree !== undefined && nodes[i].hashTree.length > 0) {
            this.stepStatus(nodes[i].hashTree);
          }
        }
      }
    },
    enableAll(source) {
      this.stepEnable = true;
      let scenarioDefinition;
      if(source==="new"){
        scenarioDefinition = this.newScenarioDefinition;
      }else {
        scenarioDefinition = this.oldScenarioDefinition;
      }
      this.stepStatus(scenarioDefinition);
    },
    disableAll(source) {
      this.stepEnable = false;
      let scenarioDefinition;
      if(source==="new"){
        scenarioDefinition = this.newScenarioDefinition;
      }else {
        scenarioDefinition = this.oldScenarioDefinition;
      }
      this.stepStatus(scenarioDefinition);
    },
    sort(stepArray, scenarioProjectId, fullPath,source) {
      if (!stepArray) {
        if(source==='new'){
          stepArray = this.newScenarioDefinition;
        }else {
          stepArray = this.oldScenarioDefinition;
        }
      }
      for (let i in stepArray) {
        stepArray[i].index = Number(i) + 1;
        if (!stepArray[i].resourceId) {
          stepArray[i].resourceId = getUUID();
        }
        // 历史数据处理
        if (stepArray[i].type === "HTTPSamplerProxy" && !stepArray[i].headers) {
          stepArray[i].headers = [new KeyValue()];
        }
        if (stepArray[i].type === ELEMENT_TYPE.LoopController
          && stepArray[i].loopType === "LOOP_COUNT"
          && stepArray[i].hashTree
          && stepArray[i].hashTree.length > 1) {
          stepArray[i].countController.proceed = true;
        }
        if (!stepArray[i].clazzName) {
          stepArray[i].clazzName = TYPE_TO_C.get(stepArray[i].type);
        }
        if (stepArray[i] && stepArray[i].authManager && !stepArray[i].authManager.clazzName) {
          stepArray[i].authManager.clazzName = TYPE_TO_C.get(stepArray[i].authManager.type);
        }
        if (!stepArray[i].projectId) {
          // 如果自身没有ID并且场景有ID则赋值场景ID，否则赋值当前项目ID
          stepArray[i].projectId = scenarioProjectId ? scenarioProjectId : this.projectId;
        } else {
          const project = this.projectList.find(p => p.id === stepArray[i].projectId);
          if (!project) {
            stepArray[i].projectId = scenarioProjectId ? scenarioProjectId : this.projectId;
          }
        }
        // 添加debug结果
        stepArray[i].parentIndex = fullPath ? fullPath + "_" + stepArray[i].index : stepArray[i].index;
        if (stepArray[i].hashTree && stepArray[i].hashTree.length > 0) {
          this.stepSize += stepArray[i].hashTree.length;
          this.sort(stepArray[i].hashTree, stepArray[i].projectId, stepArray[i].parentIndex);
        }
      }
    },
    allowDrag(draggingNode, dropNode, dropType,source) {
      if (dropNode && draggingNode && dropType) {
        this.sort(null,null,null,source);
      }
    },
    nodeExpand(data, node,source) {
      if(source==="new"){
        if (data && data.resourceId && this.newExpandedNode.indexOf(data.resourceId) === -1) {
          this.newExpandedNode.push(data.resourceId);
        }
      }else{
        if (data && data.resourceId && this.oldExpandedNode.indexOf(data.resourceId) === -1) {
          this.oldExpandedNode.push(data.resourceId);
        }
      }

    },
    nodeCollapse(data, node,source) {
      if (data && data.resourceId) {
        if(source==="new"){
          this.newExpandedNode.splice(this.newExpandedNode.indexOf(data.resourceId), 1);
        }else {
          this.oldExpandedNode.splice(this.oldExpandedNode.indexOf(data.resourceId), 1);
        }
      }
    },

  },
  created() {
    this.getWsProjects();
  }
}

</script>
<style scoped>
.compare-class{
  display: flex;
  justify-content:space-between;
}
</style>
