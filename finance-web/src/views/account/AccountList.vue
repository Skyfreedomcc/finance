<template>
  <div class="app-container" style="padding: 20px;">
    <div class="header-actions" style="margin-bottom: 20px;">
      <el-button type="primary" @click="handleAdd">新增一级科目</el-button>
      <el-button @click="getList">刷新数据</el-button>
    </div>

    <el-table
      v-loading="loading"
      :data="treeData"
      style="width: 100%;"
      row-key="accountId"
      border
      :default-expand-all="false"
      :tree-props="{children: 'children', hasChildren: 'hasChildren'}"
    >
      <el-table-column label="科目名称" min-width="200">
        <template #default="scope">
          <span v-if="scope.row.children && scope.row.children.length > 0">📂 </span>
          <span v-else>📄 </span>
          <span style="font-weight: 500">{{ scope.row.accountName }}</span>
        </template>
      </el-table-column>

      <el-table-column label="科目代码" prop="accountCode" width="120" />

      <el-table-column label="类型/方向" width="180" align="center">
        <template #default="scope">
          <!-- ✅ 修复：使用 success/warning 替代空字符串 -->
          <el-tag size="small" :type="getDirectionTagType(scope.row.balanceDirection)">
            {{ scope.row.balanceDirection === 'DEBIT' ? '借' : '贷' }}
          </el-tag>
          <el-tag
            size="small"
            :type="getAccountTypeTagType(scope.row.accountType)"
            style="margin-left: 8px;"
          >
            {{ translateType(scope.row.accountType) }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="操作" align="center" width="250">
        <template #default="scope">
          <el-button link type="primary" size="small" @click="handleAddChild(scope.row)">+子科目</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog :title="title" v-model="open" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="父级科目">
          <el-input v-model="form.parentName" disabled placeholder="顶级科目" />
        </el-form-item>
        <el-form-item label="科目名称" required>
          <el-input v-model="form.accountName" placeholder="如：交通费" />
        </el-form-item>
        <el-form-item label="科目代码" required>
          <el-input v-model="form.accountCode" placeholder="如：660201" />
        </el-form-item>
        <el-form-item label="借贷方向">
          <el-radio-group v-model="form.balanceDirection">
            <el-radio label="DEBIT">借方</el-radio>
            <el-radio label="CREDIT">贷方</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.accountType">
            <el-option label="资产" value="ASSET" />
            <el-option label="负债" value="LIABILITY" />
            <el-option label="权益" value="EQUITY" />
            <el-option label="收入" value="INCOME" />
            <el-option label="费用" value="EXPENSE" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="open = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios';
import { ElMessage, ElMessageBox } from 'element-plus';

const loading = ref(false);
const treeData = ref([]);
const open = ref(false);
const title = ref("");
const form = ref({});

// ✅ 修复：正确的 tag type 计算函数
const getDirectionTagType = (direction) => {
  // Element Plus el-tag 只接受: 'success' | 'warning' | 'info' | 'danger' | ''
  // 但空字符串也会警告，所以我们用 'success' 和 'warning'
  return direction === 'DEBIT' ? 'success' : 'warning'
}

const getAccountTypeTagType = (accountType) => {
  const typeMap = {
    'ASSET': 'primary',
    'LIABILITY': 'danger',
    'EQUITY': 'success',
    'INCOME': 'warning',
    'EXPENSE': 'info'
  }
  return typeMap[accountType] || 'info'
}

// 1. 获取列表并转树
const getList = async () => {
  loading.value = true;
  try {
    const res = await axios.get('http://localhost:8080/financeAccount/list');
    treeData.value = handleTree(res.data || [], "accountId", "parentId");
  } catch (error) {
    ElMessage.error("获取数据失败");
  } finally {
    loading.value = false;
  }
};

// 2. 提交新增
const submitForm = async () => {
  try {
    await axios.post('http://localhost:8080/financeAccount/save', form.value);
    ElMessage.success("保存成功");
    open.value = false;
    getList();
  } catch (error) {
    ElMessage.error("保存失败");
  }
};

// 3. 删除
const handleDelete = (row) => {
  ElMessageBox.confirm('是否确认删除名称为"' + row.accountName + '"的科目？', "警告", {
    type: "warning"
  }).then(async () => {
    await axios.delete(`http://localhost:8080/financeAccount/delete/${row.accountId}`);
    ElMessage.success("删除成功");
    getList();
  });
};

// 打开新增弹窗
const handleAdd = () => {
  form.value = { parentId: 0, parentName: '顶级科目', balanceDirection: 'DEBIT', accountType: 'ASSET' };
  open.value = true;
  title.value = "添加一级科目";
};

// 添加子科目
const handleAddChild = (row) => {
  form.value = {
    parentId: row.accountId,
    parentName: row.accountName,
    balanceDirection: row.balanceDirection || 'DEBIT',
    accountType: row.accountType || 'ASSET'
  };
  open.value = true;
  title.value = "添加子科目";
};

// 列表转树
function handleTree(data, id, parentId, children) {
  let config = { id: id || 'id', parentId: parentId || 'parentId', childrenList: children || 'children' };
  var childrenListMap = {};
  var nodeIds = {};
  var tree = [];
  for (let d of data) {
    let pId = d[config.parentId];
    if (childrenListMap[pId] == null) childrenListMap[pId] = [];
    nodeIds[d[config.id]] = d;
    childrenListMap[pId].push(d);
  }
  for (let d of data) {
    let pId = d[config.parentId];
    if (nodeIds[pId] == null) tree.push(d);
  }
  for (let t of tree) adaptToChildrenList(t);
  function adaptToChildrenList(o) {
    if (childrenListMap[o[config.id]] !== null) o[config.childrenList] = childrenListMap[o[config.id]];
    if (o[config.childrenList]) for (let c of o[config.childrenList]) adaptToChildrenList(c);
  }
  return tree;
}

const translateType = (type) => {
  const map = { 'ASSET': '资产', 'LIABILITY': '负债', 'EQUITY': '权益', 'INCOME': '收入', 'EXPENSE': '费用' };
  return map[type] || type || '未知';
};

onMounted(() => {
  getList();
});
</script>

<style scoped>
.header-actions {
  display: flex;
  gap: 10px;
}
</style>