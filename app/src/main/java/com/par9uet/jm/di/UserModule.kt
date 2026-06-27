package com.par9uet.jm.di

import com.par9uet.jm.repository.UserRepository
import com.par9uet.jm.repository.impl.UserRepositoryImpl
import com.par9uet.jm.ui.viewModel.UserViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

val userModule = module {
    single { UserRepositoryImpl(get(), get()) } bind UserRepository::class

    viewModel { UserViewModel(get(), get(), get(), get()) }
}
