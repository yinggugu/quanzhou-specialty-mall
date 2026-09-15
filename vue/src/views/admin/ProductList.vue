<template>
  <!-- 商品管理页面 - 1:1复刻 admin/product_list.jsp -->
  <div>
    <div class="admin-header">
      <h1 class="admin-title">商品管理</h1>
      <router-link to="/admin/products/add" class="btn btn-primary">添加商品</router-link>
    </div>

    <!-- ✅ 商品模糊搜索框：输入框 + 搜索按钮 + 重置按钮 -->
    <div class="admin-search-filter">
      <form @submit.prevent="handleSearch">
        <input
          type="text"
          v-model="searchGoodsName"
          class="form-control"
          placeholder="请输入商品名称/特产简介模糊搜索"
          @keydown.enter="handleSearch"
        />
        <button type="submit" class="btn btn-primary">搜索</button>
        <button type="button" class="btn btn-secondary" @click="handleResetSearch">重置</button>
      </form>
    </div>

    <!-- 商品表格 -->
    <div class="admin-table-container">
      <table class="table">
        <thead>
          <tr>
            <th>ID</th>
            <th>商品名称</th>
            <th>价格</th>
            <th>库存</th>
            <th>状态</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="product in products" :key="product.id">
            <td>{{ product.id }}</td>
            <td>{{ product.name }}</td>
            <td>¥{{ product.price }}</td>
            <td>{{ product.stock }}</td>
            <td>
              <span class="admin-badge" :class="product.status === 1 ? 'admin-badge-success' : 'admin-badge-danger'">
                {{ product.status === 1 ? '上架' : '下架' }}
              </span>
            </td>
            <td>{{ product.createTime | formatTime }}</td>
            <td>
              <div class="table-actions">
                <router-link
                  :to="`/admin/products/${product.id}/edit`"
                  class="btn btn-primary btn-sm"
                >编辑</router-link>
                <a
                  href="javascript:void(0)"
                  class="btn btn-danger btn-sm"
                  @click="handleDelete(product)"
                >删除</a>
                <a
                  v-if="product.status === 1"
                  href="javascript:void(0)"
                  class="btn btn-secondary btn-sm"
                  @click="handleUpdateStatus(product.id, 0)"
                >下架</a>
                <a
                  v-if="product.status === 0"
                  href="javascript:void(0)"
                  class="btn btn-success btn-sm"
                  @click="handleUpdateStatus(product.id, 1)"
                >上架</a>
              </div>
            </td>
          </tr>
          <tr v-if="products.length === 0 && !loading">
            <td colspan="7" style="text-align:center;padding:40px;">暂无数据</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 分页 -->
    <div v-if="totalPages > 1" class="admin-pagination">
      <a v-if="currentPage > 1" href="javascript:void(0)" class="btn btn-secondary" @click="changePage(currentPage - 1)">上一页</a>
      <span class="admin-pagination-info">第 {{ currentPage }} / {{ totalPages }} 页</span>
      <a v-if="currentPage < totalPages" href="javascript:void(0)" class="btn btn-secondary" @click="changePage(currentPage + 1)">下一页</a>
    </div>
  </div>
</template>

<script>
import request from '@/api/request'

export default {
  name: 'AdminProductList',

  data() {
    return {
      products: [],
      currentPage: 1,
      totalPages: 1,
      searchGoodsName: '',  // ✅ 模糊搜索关键词
      loading: false
    }
  },

  mounted() {
    this.loadProducts()
  },

  methods: {
    async loadProducts() {
      this.loading = true
      try {
        const params = { page: this.currentPage, pageSize: 10 }
        if (this.searchGoodsName) params.goodsName = this.searchGoodsName

        const res = await request.get('/admin/products', { params })
        if (res.code === 200 && res.data) {
          this.products = res.data.list || res.data.records || []
          this.totalPages = res.data.totalPages || res.data.pages || 1
          this.currentPage = res.data.currentPage || res.data.current || 1
        }
      } catch (e) {
        console.error('加载商品列表失败:', e)
      } finally {
        this.loading = false
      }
    },

    handleSearch() {
      this.currentPage = 1
      this.loadProducts()
    },

    // ✅ 重置搜索：清空输入内容，恢复加载全部商品
    handleResetSearch() {
      this.searchGoodsName = ''
      this.currentPage = 1
      this.loadProducts()
    },

    changePage(page) {
      this.currentPage = page
      this.loadProducts()
    },

    async handleDelete(product) {
      try {
        await this.$confirm(`确定要删除商品「${product.name}」吗？`, '确认删除', {
          confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
        })
        await request.delete(`/admin/products/${product.id}`)
        this.$message.success('删除成功')
        this.loadProducts()
      } catch {
        // 用户取消
      }
    },

    async handleUpdateStatus(productId, status) {
      try {
        await request.patch(`/admin/products/${productId}/status`, { status })
        this.$message.success(status === 1 ? '已上架' : '已下架')
        this.loadProducts()
      } catch (e) {
        console.error('操作失败:', e)
      }
    }
  }
}
</script>
