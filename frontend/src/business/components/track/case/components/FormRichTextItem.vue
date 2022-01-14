<template>
  <el-form-item :disable="true" :label="title" :prop="prop" :label-width="labelWidth">
    <ms-mark-down-text :prop="prop" :data="data" :disabled="disabled" :imagelinkSwitch="imagelinkSwitch"/>
  </el-form-item>
</template>

<script>
import MsMarkDownText from "@/business/components/track/case/components/MsMarkDownText";
export default {
  name: "FormRichTextItem",
  components: {MsMarkDownText},
  props: ['data', 'title', 'prop', 'disabled', 'labelWidth','imagelinkSwitch']
}
</script>

<style scoped>
</style>
