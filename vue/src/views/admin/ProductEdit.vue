<template>
  <!-- 商品添加/编辑页面 - 1:1复刻 admin/product_edit.jsp -->
  <div>
    <div class="admin-header">
      <h1 class="admin-title">{{ isEdit ? '编辑商品' : '添加商品' }}</h1>
      <router-link to="/admin/products" class="btn btn-secondary">返回商品列表</router-link>
    </div>

    <div class="card" style="margin-top: 20px;">
      <div class="card-header">
        <h2>{{ isEdit ? '编辑商品' : '商品信息' }}</h2>
      </div>
      <div class="card-body">
        <!-- 错误提示 -->
        <div v-if="errorMessage" class="alert alert-danger" role="alert">{{ errorMessage }}</div>

        <form @submit.prevent="handleSubmit">
          <!-- 商品名称 -->
          <div class="form-group">
            <label for="name">商品名称 <span style="color:red;">*</span></label>
            <input
              type="text"
              id="name"
              v-model="form.name"
              class="form-control"
              required
              placeholder="请输入商品名称"
            />
          </div>

          <!-- 商品分类 -->
          <div class="form-group">
            <label for="categoryId">商品分类 <span style="color:red;">*</span></label>
            <select id="categoryId" v-model.number="form.categoryId" class="form-control" required>
              <option value="">请选择分类</option>
              <option value="1">茶叶</option>
              <option value="2">糕点</option>
              <option value="3">海鲜</option>
              <option value="4">工艺品</option>
            </select>
          </div>

          <!-- 商品价格 -->
          <div class="form-group">
            <label for="price">商品价格 <span style="color:red;">*</span></label>
            <input
              type="number"
              id="price"
              v-model.number="form.price"
              class="form-control"
              required
              step="0.01"
              min="0"
              placeholder="请输入商品价格"
            />
          </div>

          <!-- 库存数量 -->
          <div class="form-group">
            <label for="stock">库存数量 <span style="color:red;">*</span></label>
            <input
              type="number"
              id="stock"
              v-model.number="form.stock"
              class="form-control"
              required
              min="0"
              placeholder="请输入库存数量"
            />
          </div>

          <!-- 商品图片 -->
          <div class="form-group">
            <label for="image">商品图片</label>
            <input
              type="text"
              id="image"
              v-model="form.image"
              class="form-control"
              placeholder="请输入图片路径，如：static/images/tieguanyin.jpg"
            />
            <small style="color: #666;">图片路径示例：static/images/tieguanyin.jpg</small>
            <div v-if="form.image" style="margin-top: 10px;">
              <img
                v-if="getImageUrl(form.image)"
                :src="getImageUrl(form.image)"
                alt="商品图片"
                style="max-width: 200px; max-height: 200px; border: 1px solid #ddd; border-radius: 4px;"
              />
            </div>
          </div>

          <!-- 商品描述 -->
          <div class="form-group">
            <label for="description">商品描述</label>
            <textarea
              id="description"
              v-model="form.description"
              class="form-control"
              rows="5"
              placeholder="请输入商品描述"
            ></textarea>
          </div>

          <!-- 商品状态 -->
          <div class="form-group">
            <label for="status">商品状态</label>
            <select id="status" v-model.number="form.status" class="form-control" required>
              <option :value="1">上架</option>
              <option :value="0">下架</option>
            </select>
          </div>

          <div class="form-actions" style="margin-top: 30px;">
            <button type="submit" class="btn btn-primary" :disabled="submitting">
              {{ submitting ? '保存中...' : (isEdit ? '保存修改' : '添加商品') }}
            </button>
            <router-link to="/admin/products" class="btn btn-secondary">取消</router-link>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script>
import request from '@/api/request'

export default {
  name: 'AdminProductEdit',

  data() {
    return {
      isEdit: false,
      form: {
        name: '',
        categoryId: '',
        price: 0,
        stock: 0,
        image: '',
        description: '',
        status: 1
      },
      errorMessage: '',
      submitting: false
    }
  },

  mounted() {
    const productId = this.$route.params.id
    if (productId) {
      this.isEdit = true
      this.loadProduct(productId)
    }
  },

  methods: {
    getImageUrl(imagePath) {
      return this.$resolveImageUrl(imagePath)
    },

    async loadProduct(id) {
      try {
        const res = await request.get(`/admin/products/${id}`)
        if (res.code === 200 && res.data) {
          const p = res.data
          this.form = {
            name: p.name || '',
            categoryId: p.categoryId || '',
            price: p.price || 0,
            stock: p.stock || 0,
            image: p.image || '',
            description: p.description || '',
            status: p.status != null ? p.status : 1
          }
        }
      } catch (e) {
        console.error('加载商品信息失败:', e)
      }
    },

    async handleSubmit() {
      this.errorMessage = ''

      if (!this.form.name.trim()) {
        this.errorMessage = '请输入商品名称'
        return
      }
      if (!this.form.categoryId) {
        this.errorMessage = '请选择商品分类'
        return
      }

      this.submitting = true
      try {
        let res
        if (this.isEdit) {
          res = await request.put(`/admin/products/${this.$route.params.id}`, this.form)
        } else {
          res = await request.post('/admin/products', this.form)
        }

        if (res.code === 200) {
          this.$message.success(this.isEdit ? '保存成功' : '添加成功')
          this.$router.push('/admin/products')
        }
      } catch (e) {
        this.errorMessage = e.message || '操作失败'
      } finally {
        this.submitting = false
      }
    }
  }
}
</script>
