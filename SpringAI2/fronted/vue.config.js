module.exports = {
    devServer: {
        proxy: {
            '/api': {
                target: 'http://localhost:8080', // 你的后端 Spring Boot 地址
                changeOrigin: true
            }
        }
    }
};
