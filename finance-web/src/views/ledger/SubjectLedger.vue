<script setup>
/**
 * 科目余额表（总账）- 完整修复版
 *
 * 修改记录：
 * 1. 增加了自动选中第一个节点（资产）的逻辑，进页面不再是空的。
 */
import { ref, onMounted, computed, nextTick } from 'vue' // 【修改1】引入 nextTick
import axios from 'axios'

const treeRef = ref(null) // 【修改2】定义树的引用
const treeData = ref([])
const flatAccounts = ref([])
const ledgerData = ref([])
const currentAccount = ref({})
const loading = ref(false)
const balanceMap = ref({})

const totalDebit = ref(0)
const totalCredit = ref(0)
const finalBalance = ref(0)

// 1. 加载左侧树和余额
const loadTree = async () => {
  try {
    const accountRes = await axios.get('/financeAccount/list')
    flatAccounts.value = accountRes.data || []

    const summaryRes = await axios.get('/financeTransaction/ledger/summary')
    const summaryList = summaryRes.data || []

    summaryList.forEach(item => {
      balanceMap.value[item.accountId] = {
        totalDebit: item.totalDebit || 0,
        totalCredit: item.totalCredit || 0,
        balance: item.balance || 0
      }
    })

    const tree = handleTree(flatAccounts.value, "accountId", "parentId")
    calculateParentBalances(tree)
    treeData.value = tree

    // 【修改3】核心逻辑：数据加载完成后，自动点击第一个节点
    nextTick(() => {
      if (treeData.value && treeData.value.length > 0) {
        const firstNode = treeData.value[0]

        // 1. 让左侧树高亮显示第一个节点
        if (treeRef.value) {
          treeRef.value.setCurrentKey(firstNode.accountId)
        }

        // 2. 自动触发点击事件，加载右侧数据
        handleNodeClick(firstNode)
      }
    })

  } catch (e) {
    console.error('加载数据失败', e)
  }
}

const calculateParentBalances = (nodes) => {
  nodes.forEach(node => {
    if (node.children && node.children.length > 0) {
      calculateParentBalances(node.children)
      let sumDebit = 0, sumCredit = 0, sumBalance = 0
      node.children.forEach(child => {
        const childBal = balanceMap.value[child.accountId] || { totalDebit: 0, totalCredit: 0, balance: 0 }
        sumDebit += childBal.totalDebit
        sumCredit += childBal.totalCredit
        sumBalance += childBal.balance
      })
      const selfBal = balanceMap.value[node.accountId] || { totalDebit: 0, totalCredit: 0, balance: 0 }
      balanceMap.value[node.accountId] = {
        totalDebit: sumDebit + selfBal.totalDebit,
        totalCredit: sumCredit + selfBal.totalCredit,
        balance: sumBalance + selfBal.balance
      }
    }
  })
}

const getAccountBalance = (accountId) => {
  const bal = balanceMap.value[accountId]
  if (!bal || bal.balance === 0) return ''
  return bal.balance.toFixed(2)
}

// 2. 点击左侧树
const handleNodeClick = async (data) => {
  currentAccount.value = data
  loading.value = true

  try {
    const hasChildren = data.children && data.children.length > 0

    if (hasChildren) {
      ledgerData.value = collectChildrenLedger(data)
      const bal = balanceMap.value[data.accountId] || {}
      totalDebit.value = bal.totalDebit || 0
      totalCredit.value = bal.totalCredit || 0
      finalBalance.value = bal.balance || 0
    } else {
      const res = await axios.get(`/financeTransaction/ledger/${data.accountId}`)
      if (res.data && res.data.entries) {
        ledgerData.value = res.data.entries.map(entry => ({
          date: entry.date || entry.voucherDate,
          voucherId: entry.voucherId || entry.transactionId,
          summary: entry.summary,
          dcDirection: entry.dcDirection,
          amount: entry.amount,
          balance: entry.balance
        }))
        totalDebit.value = res.data.totalDebit || 0
        totalCredit.value = res.data.totalCredit || 0
        finalBalance.value = res.data.finalBalance || 0
      } else {
        ledgerData.value = []
        totalDebit.value = 0
        totalCredit.value = 0
        finalBalance.value = 0
      }
    }
  } catch (e) {
    console.error('加载明细失败', e)
    ledgerData.value = []
  } finally {
    loading.value = false
  }
}

const collectChildrenLedger = (node) => {
  const result = []
  const collectRecursive = (n, level = 0) => {
    const bal = balanceMap.value[n.accountId] || { totalDebit: 0, totalCredit: 0, balance: 0 }
    if (bal.balance !== 0 || level === 1) {
      result.push({
        accountId: n.accountId,
        accountName: n.accountName,
        accountCode: n.accountCode,
        level: level,
        totalDebit: bal.totalDebit,
        totalCredit: bal.totalCredit,
        balance: bal.balance,
        hasChildren: n.children && n.children.length > 0
      })
    }
    if (n.children) {
      n.children.forEach(child => collectRecursive(child, level + 1))
    }
  }
  if (node.children) {
    node.children.forEach(child => collectRecursive(child, 1))
  }
  return result
}

function handleTree(data, id, parentId, children) {
  let config = { id: id || 'id', parentId: parentId || 'parentId', childrenList: children || 'children' }
  var childrenListMap = {}
  var nodeIds = {}
  var tree = []

  for (let d of data) {
    let pId = d[config.parentId]
    if (childrenListMap[pId] == null) childrenListMap[pId] = []
    nodeIds[d[config.id]] = d
    childrenListMap[pId].push(d)
  }

  for (let d of data) {
    let pId = d[config.parentId]
    if (nodeIds[pId] == null) tree.push(d)
  }

  for (let t of tree) adaptToChildrenList(t)

  function adaptToChildrenList(o) {
    if (childrenListMap[o[config.id]] !== null) {
      o[config.childrenList] = childrenListMap[o[config.id]]
    }
    if (o[config.childrenList]) {
      for (let c of o[config.childrenList]) adaptToChildrenList(c)
    }
  }
  return tree
}

const isParentView = computed(() => {
  return currentAccount.value.children && currentAccount.value.children.length > 0
})

const formatAmount = (val) => {
  if (val === null || val === undefined) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

onMounted(() => loadTree())
</script>

<template>
  <div class="ledger-layout">
    <div class="left-pane">
      <div class="pane-title">📂 科目导航</div>
      <el-tree
        ref="treeRef"
        node-key="accountId"
        :data="treeData"
        :props="{ label: 'accountName', children: 'children' }"
        default-expand-all
        highlight-current
        @node-click="handleNodeClick"
      >
        <template #default="{ node, data }">
          <div class="tree-node">
            <span>{{ data.accountName }}</span>
            <span class="node-balance" v-if="getAccountBalance(data.accountId)">
              {{ getAccountBalance(data.accountId) }}
            </span>
          </div>
        </template>
      </el-tree>
    </div>

    <div class="right-pane">
      <div class="pane-header">
        <div class="pane-title">
          <span>📄 {{ currentAccount.accountName || '资产' }} - 明细账</span>
        </div>
        <div class="balance-tag">余额: {{ formatAmount(finalBalance) }}</div>
      </div>

      <div class="summary-bar">
        <span>借方合计: <b class="debit">{{ formatAmount(totalDebit) }}</b></span>
        <span>贷方合计: <b class="credit">{{ formatAmount(totalCredit) }}</b></span>
      </div>

      <el-table
        v-if="isParentView"
        :data="ledgerData"
        border
        stripe
        height="calc(100% - 120px)"
        v-loading="loading"
      >
        <el-table-column label="科目" min-width="200">
          <template #default="scope">
            <span :style="{ paddingLeft: (scope.row.level - 1) * 20 + 'px' }">
              {{ scope.row.hasChildren ? '📂' : '📄' }}
              {{ scope.row.accountName }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="科目代码" prop="accountCode" width="120" />
        <el-table-column label="借方合计" width="150" align="right">
          <template #default="scope">
            <span v-if="scope.row.totalDebit > 0" class="debit">
              {{ formatAmount(scope.row.totalDebit) }}
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="贷方合计" width="150" align="right">
          <template #default="scope">
            <span v-if="scope.row.totalCredit > 0" class="credit">
              {{ formatAmount(scope.row.totalCredit) }}
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="余额" width="150" align="right">
          <template #default="scope">
            <b>{{ formatAmount(scope.row.balance) }}</b>
          </template>
        </el-table-column>

        <template #empty>
          <div class="empty-tip">
            <p>暂无交易记录</p>
            <p class="sub">请先在"凭证录入"或"采购/销售"模块录入业务数据</p>
          </div>
        </template>
      </el-table>

      <el-table
        v-else
        :data="ledgerData"
        border
        stripe
        height="calc(100% - 120px)"
        v-loading="loading"
      >
        <el-table-column prop="date" label="日期" width="110" sortable />
        <el-table-column prop="voucherId" label="凭证号" width="80" align="center" />
        <el-table-column prop="summary" label="业务详情 (摘要)" min-width="200" />
        <el-table-column label="借方 (Debit)" width="130" align="right">
          <template #default="scope">
            <span v-if="scope.row.dcDirection === 1" class="debit">
              {{ formatAmount(scope.row.amount) }}
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="贷方 (Credit)" width="130" align="right">
          <template #default="scope">
            <span v-if="scope.row.dcDirection === -1" class="credit">
              {{ formatAmount(scope.row.amount) }}
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="余额" width="130" align="right">
          <template #default="scope">
            <b>{{ formatAmount(scope.row.balance) }}</b>
          </template>
        </el-table-column>

        <template #empty>
          <div class="empty-tip">
            <p>暂无交易记录</p>
            <p class="sub">请先在"凭证录入"或"采购/销售"模块录入业务数据</p>
          </div>
        </template>
      </el-table>
    </div>
  </div>
</template>

<style scoped>
.ledger-layout {
  display: flex;
  height: 85vh;
  border: 1px solid #dcdfe6;
  background: white;
}
.left-pane {
  width: 300px;
  border-right: 1px solid #eee;
  overflow-y: auto;
  background: #fdfdfd;
}
.right-pane {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.pane-title {
  padding: 12px 15px;
  background: #f5f7fa;
  border-bottom: 1px solid #eee;
  font-weight: bold;
  font-size: 14px;
}
.pane-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-right: 15px;
  background: #f5f7fa;
  border-bottom: 1px solid #eee;
}
.balance-tag {
  background: #e1f3d8;
  color: #67c23a;
  padding: 4px 12px;
  border-radius: 4px;
  font-weight: bold;
}
.summary-bar {
  display: flex;
  gap: 30px;
  padding: 10px 15px;
  background: #fafafa;
  border-bottom: 1px solid #eee;
  font-size: 14px;
}
.summary-bar b {
  font-size: 16px;
  margin-left: 5px;
}
.tree-node {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding-right: 10px;
}
.node-balance {
  font-size: 12px;
  color: #67c23a;
  background: #f0f9eb;
  padding: 2px 6px;
  border-radius: 3px;
}
.debit {
  color: #67c23a;
  font-weight: 500;
}
.credit {
  color: #f56c6c;
  font-weight: 500;
}
.empty-tip {
  padding: 40px;
  text-align: center;
  color: #999;
}
.empty-tip .sub {
  font-size: 12px;
  margin-top: 10px;
}
</style>